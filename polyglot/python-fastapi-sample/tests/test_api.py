#
# Copyright © ${project.inceptionYear} organization baomidou
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""与 rust-axum-sample、typescript-hono-sample 相同的一组场景，分别跑在 H2 与 Testcontainers 启动的 PostgreSQL 上。

isolate 与 dd_open 都是进程级单例，两个测试依次打开、关闭各自的 DynamicRoutingDataSource。
"""

from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from typing import Any

from fastapi.testclient import TestClient
from httpx2 import Response
from testcontainers.community.postgres import PostgresContainer

from app.config import SAMPLE_DIR, load_config
from app.main import create_app


def test_h2() -> None:
    slave_3 = {
        "name": "slave_3",
        "url": "jdbc:h2:mem:ds_slave_3;DB_CLOSE_DELAY=-1",
        "username": "sa",
        "password": "",
        "driver_class_name": "org.h2.Driver",
    }
    run_scenarios(load_config("h2"), slave_3)


def test_postgresql() -> None:
    # 与 compose.yaml 一样，initdb 阶段执行 init-databases.sql，为每个数据源建一个数据库
    postgres = PostgresContainer("postgres:18.6-trixie", username="postgres", password="postgres")
    postgres.with_copy_into_container(
        SAMPLE_DIR / "db" / "postgresql" / "init-databases.sql", "/docker-entrypoint-initdb.d/init-databases.sql"
    )
    with postgres:
        host = postgres.get_container_host_ip()
        port = str(postgres.get_exposed_port(5432))
        variables = {
            "POSTGRES_HOST": host,
            "POSTGRES_PORT": port,
            "POSTGRES_USER": "postgres",
            "POSTGRES_PASSWORD": "postgres",
        }
        slave_3 = {
            "name": "slave_3",
            "url": f"jdbc:postgresql://{host}:{port}/ds_slave_3",
            "username": "postgres",
            "password": "postgres",
            "driver_class_name": "org.postgresql.Driver",
        }
        run_scenarios(load_config("postgresql", variables), slave_3)


def order(product_id: int, amount: int) -> dict[str, int]:
    return {"user_id": 1, "product_id": product_id, "amount": amount}


def names(response: Response) -> list[str]:
    return [user["name"] for user in response.json()]


def assert_order_state(client: TestClient, orders: int, stock: int, balance: float) -> None:
    assert len(client.get("/orders").json()) == orders
    assert client.get("/products/1").json()["stock"] == stock
    assert client.get("/accounts/1").json()["balance"] == balance


def run_scenarios(config: dict[str, Any], slave_3: dict[str, Any]) -> None:
    """动态添加的 slave_3 由调用方给出，因为它的连接信息随数据库不同。"""
    # 进入时触发 lifespan 里的 dd_open，退出时 dd_close
    with TestClient(create_app(config)) as client:
        # 读请求默认走 slave 组，两次请求轮询到两个成员
        served = set()
        for _ in range(2):
            response = client.get("/users")
            assert response.status_code == 200
            datasource = response.headers["X-Datasource-Served"]
            assert response.json()[0]["name"] == f"{datasource}-张三"
            served.add(datasource)
        assert served == {"slave_1", "slave_2"}

        # 请求头指定数据源；strict 模式下未知数据源直接报错
        response = client.get("/users", headers={"X-Datasource": "master"})
        assert response.headers["X-Datasource-Served"] == "master"
        assert response.json()[0]["name"] == "master-张三"
        response = client.get("/users", headers={"X-Datasource": "nope"})
        assert response.status_code == 404
        assert response.json()["code"] == "DATASOURCE_NOT_FOUND"

        # 写请求走 primary
        response = client.post("/users", json={"name": "李四", "age": 20})
        assert response.status_code == 201
        assert response.json()["datasource"] == "master"
        assert isinstance(response.json()["id"], int)
        assert "李四" in names(client.get("/users", headers={"X-Datasource": "master"}))
        assert "李四" not in names(client.get("/users", headers={"X-Datasource": "slave_1"}))

        # 运行时增删数据源，slave_3 自动进入 slave 组
        response = client.post("/datasources", json=slave_3)
        assert response.status_code == 201, response.text
        assert response.json()["groups"]["slave"] == ["slave_1", "slave_2", "slave_3"]
        assert len({client.get("/users").headers["X-Datasource-Served"] for _ in range(3)}) == 3
        response = client.delete("/datasources/slave_3")
        assert response.status_code == 200
        assert response.json()["groups"]["slave"] == ["slave_1", "slave_2"]
        assert client.delete("/datasources/master").status_code == 400

        # 会话式事务下单：与 tx-local-sample 相同的流程，失败时三个库一起回滚
        response = client.post("/orders", json=order(1, 1))
        assert response.status_code == 201, response.text
        assert response.json()["total_price"] == 10.0
        assert_order_state(client, 1, 19, 40.0)
        response = client.post("/orders", json=order(1, 6))
        assert response.status_code == 409
        assert response.json()["code"] == "INSUFFICIENT_BALANCE"
        assert client.post("/orders", json=order(1, 30)).json()["code"] == "INSUFFICIENT_STOCK"
        assert client.post("/orders", json=order(999, 1)).status_code == 404
        assert_order_state(client, 1, 19, 40.0)

        # 批量事务下单：一次 dd_transaction，CHECK 约束兜底
        response = client.post("/orders/batch", json=order(1, 1))
        assert response.status_code == 201, response.text
        assert_order_state(client, 2, 18, 30.0)
        response = client.post("/orders/batch", json=order(1, 6))
        assert response.status_code == 409
        assert response.json()["code"] == "INSUFFICIENT_BALANCE"
        assert client.post("/orders/batch", json=order(1, 30)).json()["code"] == "INSUFFICIENT_STOCK"
        assert_order_state(client, 2, 18, 30.0)

        # 并发请求落在 FastAPI 线程池的多个线程上，各自 attach 到 isolate
        with ThreadPoolExecutor(max_workers=8) as pool:
            statuses = list(pool.map(lambda _: client.get("/users").status_code, range(20)))
        assert statuses == [200] * 20
