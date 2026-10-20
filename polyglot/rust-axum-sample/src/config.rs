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
//! 本示例自带的数据源配置与 SQL 脚本：crate 根目录下的 `config/` 与 `db/`。

use serde_json::{Value, json};
use std::path::{Path, PathBuf};
use std::{env, fs};

/// 存放 `config/` 与 `db/` 的目录：默认是 crate 根目录，容器里用 `SAMPLE_DIR` 指定。
pub fn sample_dir() -> PathBuf {
    env::var_os("SAMPLE_DIR")
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from(env!("CARGO_MANIFEST_DIR")))
        .canonicalize()
        .expect("sample directory exists")
}

/// 读取 `config/<profile>.json`，并通过 `variables` 告诉 dd_open 建表脚本的位置。
pub fn load_config(sample: &Path, profile: &str) -> Value {
    let path = sample.join("config").join(format!("{profile}.json"));
    let text =
        fs::read_to_string(&path).unwrap_or_else(|e| panic!("cannot read {}: {e}", path.display()));
    let mut config: Value = serde_json::from_str(&text).expect("configuration is valid JSON");
    config["variables"] = json!({ "DB_DIR": sample.join("db") });
    config
}
