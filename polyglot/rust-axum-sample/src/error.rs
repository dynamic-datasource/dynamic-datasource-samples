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
//! 错误响应 `{"code","message"}`，三个示例的错误码一致；相当于 tx-local-sample 的 `GlobalExceptionHandler`。

use crate::dynamic_datasource::{Error, ErrorCode};
use axum::Json;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use serde_json::json;

pub struct ApiError {
    status: StatusCode,
    code: String,
    message: String,
}

impl ApiError {
    pub fn new(status: StatusCode, code: &str, message: impl Into<String>) -> Self {
        Self {
            status,
            code: code.to_owned(),
            message: message.into(),
        }
    }
}

/// dynamic-datasource 的错误码到 HTTP 状态码，其余一律 500。
impl From<Error> for ApiError {
    fn from(error: Error) -> Self {
        let message = error.message.to_lowercase();
        match error.code {
            // 批量事务里库存、余额不足由 CHECK 约束兜底，按约束名还原成与会话式事务相同的业务错误
            ErrorCode::ConstraintViolation if message.contains("ck_product_stock") => {
                Self::new(StatusCode::CONFLICT, "INSUFFICIENT_STOCK", "库存不足")
            }
            ErrorCode::ConstraintViolation if message.contains("ck_account_balance") => {
                Self::new(StatusCode::CONFLICT, "INSUFFICIENT_BALANCE", "余额不足")
            }
            code => {
                let status = match code {
                    ErrorCode::BadRequest => StatusCode::BAD_REQUEST,
                    ErrorCode::DatasourceNotFound => StatusCode::NOT_FOUND,
                    ErrorCode::ConstraintViolation | ErrorCode::UpdateCountMismatch => {
                        StatusCode::CONFLICT
                    }
                    _ => StatusCode::INTERNAL_SERVER_ERROR,
                };
                Self::new(status, code.as_str(), error.message)
            }
        }
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let body = json!({ "code": self.code, "message": self.message });
        (self.status, Json(body)).into_response()
    }
}
