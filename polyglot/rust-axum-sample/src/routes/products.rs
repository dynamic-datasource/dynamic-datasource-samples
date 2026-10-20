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
//! `/products`，以及下单时扣减库存，对应 tx-local-sample 的 `ProductService`。

use super::AppState;
use crate::dynamic_datasource::{Transaction, first_row};
use crate::error::ApiError;
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use serde_json::{Value, json};

pub(super) fn router() -> Router<AppState> {
    Router::new().route("/products/{id}", get(get_product))
}

/// GET /products/{id}
async fn get_product(
    State(state): State<AppState>,
    Path(id): Path<i64>,
) -> Result<Json<Value>, ApiError> {
    let result = state
        .dd
        .execute(json!({
            "ds": "product",
            "sql": "select id, price, stock from product where id = ?",
            "params": [id],
        }))
        .await?;
    first_row(result).map(Json).ok_or_else(not_found)
}

/// 对应 `ProductService#reduceStock`：检查并扣减库存，返回总价。
pub(super) async fn reduce_stock(
    tx: &Transaction,
    product_id: i64,
    amount: i64,
) -> Result<f64, ApiError> {
    let product = first_row(
        tx.execute(json!({
            "ds": "product",
            "sql": "select price, stock from product where id = ? for update",
            "params": [product_id],
        }))
        .await?,
    )
    .ok_or_else(not_found)?;
    if product["stock"].as_i64().unwrap_or_default() < amount {
        return Err(ApiError::new(
            StatusCode::CONFLICT,
            "INSUFFICIENT_STOCK",
            "库存不足",
        ));
    }
    tx.execute(json!({
        "ds": "product",
        "sql": "update product set stock = stock - ? where id = ?",
        "params": [amount, product_id],
    }))
    .await?;
    Ok(product["price"].as_f64().unwrap_or_default() * amount as f64)
}

fn not_found() -> ApiError {
    ApiError::new(StatusCode::NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在")
}
