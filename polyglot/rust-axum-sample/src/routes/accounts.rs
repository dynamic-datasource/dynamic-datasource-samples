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
//! `/accounts`，以及下单时扣减余额，对应 tx-local-sample 的 `AccountService`。

use super::AppState;
use crate::dynamic_datasource::{Transaction, first_row};
use crate::error::ApiError;
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use serde_json::{Value, json};

pub(super) fn router() -> Router<AppState> {
    Router::new().route("/accounts/{id}", get(get_account))
}

/// GET /accounts/{id}
async fn get_account(
    State(state): State<AppState>,
    Path(id): Path<i64>,
) -> Result<Json<Value>, ApiError> {
    let result = state
        .dd
        .execute(json!({
            "ds": "account",
            "sql": "select id, balance from account where id = ?",
            "params": [id],
        }))
        .await?;
    first_row(result).map(Json).ok_or_else(not_found)
}

/// 对应 `AccountService#reduceBalance`：检查并扣减余额。
pub(super) async fn reduce_balance(
    tx: &Transaction,
    user_id: i64,
    total_price: f64,
) -> Result<(), ApiError> {
    let account = first_row(
        tx.execute(json!({
            "ds": "account",
            "sql": "select balance from account where id = ? for update",
            "params": [user_id],
        }))
        .await?,
    )
    .ok_or_else(not_found)?;
    if account["balance"].as_f64().unwrap_or_default() < total_price {
        return Err(ApiError::new(
            StatusCode::CONFLICT,
            "INSUFFICIENT_BALANCE",
            "余额不足",
        ));
    }
    tx.execute(json!({
        "ds": "account",
        "sql": "update account set balance = balance - ? where id = ?",
        "params": [total_price, user_id],
    }))
    .await?;
    Ok(())
}

fn not_found() -> ApiError {
    ApiError::new(StatusCode::NOT_FOUND, "ACCOUNT_NOT_FOUND", "用户不存在")
}
