/*
 * Copyright © ${project.inceptionYear} organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//! 与 python-fastapi-sample、typescript-hono-sample 相同的一组场景，分别跑在 H2 与
//! Testcontainers 启动的 PostgreSQL 上。

use axum::Router;
use axum::body::{Body, to_bytes};
use axum::http::{Method, Request, StatusCode};
use rust_axum_sample::config::{load_config, sample_dir};
use rust_axum_sample::dynamic_datasource::DynamicDataSource;
use rust_axum_sample::routes;
use serde_json::{Value, json};
use std::collections::BTreeSet;
use testcontainers_modules::postgres::Postgres;
use testcontainers_modules::testcontainers::ImageExt;
use testcontainers_modules::testcontainers::runners::AsyncRunner;
use tokio::sync::Mutex;
use tokio::task::JoinSet;
use tower::ServiceExt;

/// isolate 与 dd_open 都是进程级单例，两个测试不能同时打开。
static OPEN: Mutex<()> = Mutex::const_new(());

#[tokio::test(flavor = "multi_thread")]
async fn h2() {
    let _open = OPEN.lock().await;
    let slave_3 = json!({
        "name": "slave_3",
        "url": "jdbc:h2:mem:ds_slave_3;DB_CLOSE_DELAY=-1",
        "username": "sa",
        "password": "",
        "driver_class_name": "org.h2.Driver",
    });
    run_scenarios(load_config(&sample_dir(), "h2"), slave_3).await;
}

#[tokio::test(flavor = "multi_thread")]
async fn postgresql() {
    let _open = OPEN.lock().await;
    let sample = sample_dir();
    // 与 compose.yaml 一样，initdb 阶段执行 init-databases.sql，为每个数据源建一个数据库
    let postgres = Postgres::default()
        .with_init_sql(sample.join("db/postgresql/init-databases.sql"))
        .with_tag("18.6-trixie")
        .start()
        .await
        .unwrap();
    // 在容器里跑测试时 testcontainers-rs 会改用 bridge 网关，rootless Docker 下它连不到映射端口；
    // 与 Python、Node 的 Testcontainers 一样认 TESTCONTAINERS_HOST_OVERRIDE（见 compose.yaml 的 rust-test）
    let host = match std::env::var("TESTCONTAINERS_HOST_OVERRIDE") {
        Ok(host) => host,
        Err(_) => postgres.get_host().await.unwrap().to_string(),
    };
    let port = postgres.get_host_port_ipv4(5432).await.unwrap().to_string();
    let mut config = load_config(&sample, "postgresql");
    config["variables"]["POSTGRES_HOST"] = json!(host);
    config["variables"]["POSTGRES_PORT"] = json!(port);
    config["variables"]["POSTGRES_USER"] = json!("postgres");
    config["variables"]["POSTGRES_PASSWORD"] = json!("postgres");
    let slave_3 = json!({
        "name": "slave_3",
        "url": format!("jdbc:postgresql://{host}:{port}/ds_slave_3"),
        "username": "postgres",
        "password": "postgres",
        "driver_class_name": "org.postgresql.Driver",
    });
    run_scenarios(config, slave_3).await;
}

/// 动态添加的 slave_3 由调用方给出，因为它的连接信息随数据库不同。
async fn run_scenarios(config: Value, slave_3: Value) {
    let dd = DynamicDataSource::open(config).await.unwrap();
    let app = routes::router(dd, &sample_dir().join("db"));

    // 读请求默认走 slave 组，两次请求轮询到两个成员
    let mut served = BTreeSet::new();
    for _ in 0..2 {
        let reply = get(&app, "/users", None).await;
        assert_eq!(reply.status, StatusCode::OK);
        let datasource = reply.served.unwrap();
        assert_eq!(reply.body[0]["name"], format!("{datasource}-张三"));
        served.insert(datasource);
    }
    assert_eq!(
        served,
        BTreeSet::from(["slave_1".to_owned(), "slave_2".to_owned()])
    );

    // 请求头指定数据源；strict 模式下未知数据源直接报错
    let reply = get(&app, "/users", Some("master")).await;
    assert_eq!(reply.served.as_deref(), Some("master"));
    assert_eq!(reply.body[0]["name"], "master-张三");
    let reply = get(&app, "/users", Some("nope")).await;
    assert_eq!(reply.status, StatusCode::NOT_FOUND);
    assert_eq!(reply.body["code"], "DATASOURCE_NOT_FOUND");

    // 写请求走 primary
    let reply = post(&app, "/users", json!({ "name": "李四", "age": 20 })).await;
    assert_eq!(reply.status, StatusCode::CREATED);
    assert_eq!(reply.body["datasource"], "master");
    assert!(reply.body["id"].is_i64());
    assert!(names(&get(&app, "/users", Some("master")).await).contains(&"李四".to_owned()));
    assert!(!names(&get(&app, "/users", Some("slave_1")).await).contains(&"李四".to_owned()));

    // 运行时增删数据源，slave_3 自动进入 slave 组
    let reply = post(&app, "/datasources", slave_3).await;
    assert_eq!(reply.status, StatusCode::CREATED, "{}", reply.body);
    assert_eq!(
        reply.body["groups"]["slave"],
        json!(["slave_1", "slave_2", "slave_3"])
    );
    let mut served = BTreeSet::new();
    for _ in 0..3 {
        served.insert(get(&app, "/users", None).await.served.unwrap());
    }
    assert_eq!(served.len(), 3);
    let reply = send(&app, Method::DELETE, "/datasources/slave_3", None, None).await;
    assert_eq!(reply.status, StatusCode::OK);
    assert_eq!(reply.body["groups"]["slave"], json!(["slave_1", "slave_2"]));
    let reply = send(&app, Method::DELETE, "/datasources/master", None, None).await;
    assert_eq!(reply.status, StatusCode::BAD_REQUEST);

    // 会话式事务下单：与 tx-local-sample 相同的流程，失败时三个库一起回滚
    let reply = post(&app, "/orders", order(1, 1)).await;
    assert_eq!(reply.status, StatusCode::CREATED, "{}", reply.body);
    assert_eq!(reply.body["total_price"], 10.0);
    assert_order_state(&app, 1, 19, 40.0).await;
    let reply = post(&app, "/orders", order(1, 6)).await;
    assert_eq!(reply.status, StatusCode::CONFLICT);
    assert_eq!(reply.body["code"], "INSUFFICIENT_BALANCE");
    let reply = post(&app, "/orders", order(1, 30)).await;
    assert_eq!(reply.body["code"], "INSUFFICIENT_STOCK");
    let reply = post(&app, "/orders", order(999, 1)).await;
    assert_eq!(reply.status, StatusCode::NOT_FOUND);
    assert_order_state(&app, 1, 19, 40.0).await;

    // 批量事务下单：一次 dd_transaction，CHECK 约束兜底
    let reply = post(&app, "/orders/batch", order(1, 1)).await;
    assert_eq!(reply.status, StatusCode::CREATED, "{}", reply.body);
    assert_order_state(&app, 2, 18, 30.0).await;
    let reply = post(&app, "/orders/batch", order(1, 6)).await;
    assert_eq!(reply.status, StatusCode::CONFLICT);
    assert_eq!(reply.body["code"], "INSUFFICIENT_BALANCE");
    let reply = post(&app, "/orders/batch", order(1, 30)).await;
    assert_eq!(reply.body["code"], "INSUFFICIENT_STOCK");
    assert_order_state(&app, 2, 18, 30.0).await;

    // 并发请求分散到 blocking 线程池的多个线程上，各自 attach 到 isolate
    let mut requests = JoinSet::new();
    for _ in 0..20 {
        let app = app.clone();
        requests.spawn(async move { get(&app, "/users", None).await.status });
    }
    while let Some(status) = requests.join_next().await {
        assert_eq!(status.unwrap(), StatusCode::OK);
    }

    dd.close().await.unwrap();
}

struct Reply {
    status: StatusCode,
    served: Option<String>,
    body: Value,
}

async fn send(
    app: &Router,
    method: Method,
    uri: &str,
    datasource: Option<&str>,
    body: Option<Value>,
) -> Reply {
    let mut request = Request::builder().method(method).uri(uri);
    if let Some(datasource) = datasource {
        request = request.header("x-datasource", datasource);
    }
    let request = match body {
        Some(body) => request
            .header("content-type", "application/json")
            .body(Body::from(body.to_string())),
        None => request.body(Body::empty()),
    }
    .unwrap();
    let response = app.clone().oneshot(request).await.unwrap();
    let status = response.status();
    let served = response
        .headers()
        .get("x-datasource-served")
        .map(|value| value.to_str().unwrap().to_owned());
    let bytes = to_bytes(response.into_body(), usize::MAX).await.unwrap();
    let body = serde_json::from_slice(&bytes)
        .unwrap_or_else(|_| Value::String(String::from_utf8_lossy(&bytes).into_owned()));
    Reply {
        status,
        served,
        body,
    }
}

async fn get(app: &Router, uri: &str, datasource: Option<&str>) -> Reply {
    send(app, Method::GET, uri, datasource, None).await
}

async fn post(app: &Router, uri: &str, body: Value) -> Reply {
    send(app, Method::POST, uri, None, Some(body)).await
}

fn names(reply: &Reply) -> Vec<String> {
    reply
        .body
        .as_array()
        .unwrap()
        .iter()
        .map(|user| user["name"].as_str().unwrap().to_owned())
        .collect()
}

fn order(product_id: i64, amount: i64) -> Value {
    json!({ "user_id": 1, "product_id": product_id, "amount": amount })
}

async fn assert_order_state(app: &Router, orders: usize, stock: i64, balance: f64) {
    assert_eq!(
        get(app, "/orders", None)
            .await
            .body
            .as_array()
            .unwrap()
            .len(),
        orders
    );
    assert_eq!(get(app, "/products/1", None).await.body["stock"], stock);
    assert_eq!(get(app, "/accounts/1", None).await.body["balance"], balance);
}
