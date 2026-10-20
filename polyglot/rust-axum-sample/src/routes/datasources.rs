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
//! `/datasources`：运行时增删数据源，对应 add-remove-datasource-sample。

use super::AppState;
use crate::error::ApiError;
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::{delete, get};
use axum::{Json, Router};
use serde::Deserialize;
use serde_json::{Value, json};

pub(super) fn router() -> Router<AppState> {
    Router::new()
        .route("/datasources", get(list_datasources).post(add_datasource))
        .route("/datasources/{name}", delete(remove_datasource))
}

/// GET /datasources：当前的数据源与分组。
async fn list_datasources(State(state): State<AppState>) -> Result<Json<Value>, ApiError> {
    Ok(Json(state.dd.list_datasources().await?))
}

#[derive(Deserialize)]
struct NewDatasource {
    name: String,
    url: String,
    username: Option<String>,
    password: Option<String>,
    driver_class_name: Option<String>,
}

/// POST /datasources：名称带下划线时自动入组（slave_3 → slave）。
async fn add_datasource(
    State(state): State<AppState>,
    Json(datasource): Json<NewDatasource>,
) -> Result<(StatusCode, Json<Value>), ApiError> {
    let result = state
        .dd
        .add_datasource(json!({
            "name": datasource.name,
            "url": datasource.url,
            "username": datasource.username,
            "password": datasource.password,
            "driverClassName": datasource.driver_class_name,
            "init": { "schema": &*state.user_schema },
        }))
        .await?;
    Ok((StatusCode::CREATED, Json(result)))
}

/// DELETE /datasources/{name}：删除数据源并关闭它的连接池，primary 不能删除。
async fn remove_datasource(
    State(state): State<AppState>,
    Path(name): Path<String>,
) -> Result<Json<Value>, ApiError> {
    Ok(Json(state.dd.remove_datasource(&name).await?))
}
