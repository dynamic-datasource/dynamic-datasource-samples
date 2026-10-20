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
//! HTTP 接口，与 python-fastapi-sample、typescript-hono-sample 完全一致，约定见 `../README.md`。
//!
//! 每个资源一个子模块，各自用 `router()` 声明路由；这里把它们合并起来，并注入共享的状态。

mod accounts;
mod datasources;
mod orders;
mod products;
mod users;

use crate::dynamic_datasource::DynamicDataSource;
use axum::Router;
use std::path::Path;
use std::sync::Arc;

#[derive(Clone)]
struct AppState {
    dd: DynamicDataSource,
    /// 新加入的从库沿用 t_user 的建表脚本：脚本路径由服务端决定，不接受客户端传入。
    user_schema: Arc<str>,
}

pub fn router(dd: DynamicDataSource, db_dir: &Path) -> Router {
    let user_schema = format!("file:{}", db_dir.join("user/schema.sql").display()).into();
    Router::new()
        .merge(users::router())
        .merge(datasources::router())
        .merge(orders::router())
        .merge(products::router())
        .merge(accounts::router())
        .with_state(AppState { dd, user_schema })
}
