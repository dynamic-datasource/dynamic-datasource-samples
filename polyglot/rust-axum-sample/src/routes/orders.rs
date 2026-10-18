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
//! `/orders`：多数据源本地事务，对应 tx-local-sample 的 `OrderController` 与 `OrderService`。

use super::AppState;
use super::accounts::reduce_balance;
use super::products::reduce_stock;
use crate::dynamic_datasource::{Transaction, first_row};
use crate::error::ApiError;
use axum::extract::State;
use axum::http::StatusCode;
use axum::routing::{get, post};
use axum::{Json, Router};
use serde::Deserialize;
use serde_json::{Value, json};

pub(super) fn router() -> Router<AppState> {
    Router::new()
        .route("/orders", get(list_orders).post(place_order))
        .route("/orders/batch", post(place_order_batch))
}

#[derive(Deserialize)]
struct PlaceOrder {
    user_id: i64,
    product_id: i64,
    amount: i64,
}

impl PlaceOrder {
    fn validate(&self) -> Result<(), ApiError> {
        if self.amount > 0 {
            Ok(())
        } else {
            Err(ApiError::new(
                StatusCode::BAD_REQUEST,
                "BAD_REQUEST",
                "amount 必须大于 0",
            ))
        }
    }

    fn placed(&self, id: Value, total_price: f64) -> Value {
        json!({
            "id": id,
            "user_id": self.user_id,
            "product_id": self.product_id,
            "amount": self.amount,
            "total_price": total_price,
            "status": "SUCCESS",
        })
    }
}

/// POST /orders：与 `OrderService#placeOrder` 逐行对应，
/// 一个会话式事务跨 order、product、account 三个库，任何一步失败都整体回滚。
async fn place_order(
    State(state): State<AppState>,
    Json(order): Json<PlaceOrder>,
) -> Result<(StatusCode, Json<Value>), ApiError> {
    order.validate()?;
    let placed = state
        .dd
        .transactional(async |tx: &Transaction| -> Result<Value, ApiError> {
            let inserted = tx
                .execute(json!({
                    "ds": "order",
                    "sql": "insert into p_order (user_id, product_id, amount, status) values (?, ?, ?, 'INIT')",
                    "params": [order.user_id, order.product_id, order.amount],
                    "generatedKeys": ["id"],
                }))
                .await?;
            let order_id = inserted["generatedKeys"][0]["id"].clone();
            let total_price = reduce_stock(tx, order.product_id, order.amount).await?;
            reduce_balance(tx, order.user_id, total_price).await?;
            tx.execute(json!({
                "ds": "order",
                "sql": "update p_order set status = 'SUCCESS', total_price = ? where id = ?",
                "params": [total_price, order_id],
            }))
            .await?;
            Ok(order.placed(order_id, total_price))
        })
        .await?;
    Ok((StatusCode::CREATED, Json(placed)))
}

/// POST /orders/batch：先在事务外读单价，再把三条写语句一次交给 `dd_transaction`。
/// 库存、余额不足由 CHECK 约束兜底；单价在下单期间被改过则 `expectUpdateCount` 不满足，同样整体回滚。
async fn place_order_batch(
    State(state): State<AppState>,
    Json(order): Json<PlaceOrder>,
) -> Result<(StatusCode, Json<Value>), ApiError> {
    order.validate()?;
    let product = first_row(
        state
            .dd
            .execute(json!({
                "ds": "product",
                "sql": "select price from product where id = ?",
                "params": [order.product_id],
            }))
            .await?,
    )
    .ok_or_else(|| ApiError::new(StatusCode::NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在"))?;
    let price = product["price"].as_f64().unwrap_or_default();
    let total_price = price * order.amount as f64;
    let result = state
        .dd
        .transaction(vec![
            json!({
                "ds": "order",
                "sql": "insert into p_order (user_id, product_id, amount, total_price, status) values (?, ?, ?, ?, 'SUCCESS')",
                "params": [order.user_id, order.product_id, order.amount, total_price],
                "generatedKeys": ["id"],
            }),
            json!({
                "ds": "product",
                "sql": "update product set stock = stock - ? where id = ? and price = ?",
                "params": [order.amount, order.product_id, price],
                "expectUpdateCount": 1,
            }),
            json!({
                "ds": "account",
                "sql": "update account set balance = balance - ? where id = ?",
                "params": [total_price, order.user_id],
                "expectUpdateCount": 1,
            }),
        ])
        .await?;
    let order_id = result["results"][0]["generatedKeys"][0]["id"].clone();
    Ok((
        StatusCode::CREATED,
        Json(order.placed(order_id, total_price)),
    ))
}

/// GET /orders
async fn list_orders(State(state): State<AppState>) -> Result<Json<Value>, ApiError> {
    let mut result = state
        .dd
        .execute(json!({
            "ds": "order",
            "sql": "select id, user_id, product_id, amount, total_price, status from p_order order by id",
        }))
        .await?;
    Ok(Json(result["rows"].take()))
}
