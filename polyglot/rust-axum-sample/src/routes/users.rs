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
//! `/users`：读写分离与按请求头切换数据源，对应 jdbc-template-sample 与 spel-sample。

use super::AppState;
use crate::error::ApiError;
use axum::extract::{FromRequestParts, State};
use axum::http::StatusCode;
use axum::http::request::Parts;
use axum::response::{IntoResponse, Response};
use axum::routing::get;
use axum::{Json, Router};
use serde::Deserialize;
use serde_json::{Value, json};
use std::convert::Infallible;

pub(super) fn router() -> Router<AppState> {
    Router::new().route("/users", get(list_users).post(create_user))
}

/// 请求头 `X-Datasource`，相当于 spel-sample 里的 `@DS("#header.xxx")`。
struct RequestedDatasource(Option<String>);

impl<S: Send + Sync> FromRequestParts<S> for RequestedDatasource {
    type Rejection = Infallible;

    async fn from_request_parts(parts: &mut Parts, _: &S) -> Result<Self, Self::Rejection> {
        let datasource = parts.headers.get("x-datasource");
        Ok(Self(
            datasource
                .and_then(|value| value.to_str().ok())
                .map(str::to_owned),
        ))
    }
}

/// GET /users：默认读 slave 组（相当于 `@DS("slave")`），响应头回显实际命中的成员。
async fn list_users(
    State(state): State<AppState>,
    RequestedDatasource(datasource): RequestedDatasource,
) -> Result<Response, ApiError> {
    let mut result = state
        .dd
        .execute(json!({
            "ds": datasource.as_deref().unwrap_or("slave"),
            "sql": "select id, name, age from t_user order by id",
        }))
        .await?;
    let served = result["datasource"].as_str().unwrap_or_default().to_owned();
    Ok((
        [("x-datasource-served", served)],
        Json(result["rows"].take()),
    )
        .into_response())
}

#[derive(Deserialize)]
struct NewUser {
    name: String,
    age: Option<i32>,
}

/// POST /users：不指定数据源时写 primary，也就是 master。
async fn create_user(
    State(state): State<AppState>,
    Json(user): Json<NewUser>,
) -> Result<(StatusCode, Json<Value>), ApiError> {
    let result = state
        .dd
        .execute(json!({
            "sql": "insert into t_user (name, age) values (?, ?)",
            "params": [user.name, user.age],
            "generatedKeys": ["id"],
        }))
        .await?;
    Ok((
        StatusCode::CREATED,
        Json(json!({
            "id": result["generatedKeys"][0]["id"],
            "name": user.name,
            "age": user.age,
            "datasource": result["datasource"],
        })),
    ))
}
