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
//! libdynamicds（dynamic-datasource 的 GraalVM native 共享库）的安全封装。
//!
//! GraalVM 要求调用入口的 OS 线程已经 attach 到 isolate：每个线程第一次调用时 attach，线程退出时由
//! 线程局部变量的析构函数 detach。C 函数都是阻塞调用，这里一律经 [`tokio::task::spawn_blocking`]
//! 放到 blocking 线程池执行，不占用 tokio 的工作线程。

use serde::Deserialize;
use serde_json::{Value, json};
use std::ffi::{CStr, CString, c_char, c_int};
use std::fmt;
use std::sync::OnceLock;

#[repr(C)]
struct GraalIsolate {
    _private: [u8; 0],
}

#[repr(C)]
struct GraalIsolateThread {
    _private: [u8; 0],
}

/// 业务函数的统一签名：`char* dd_xxx(graal_isolatethread_t* thread, const char* request)`。
type Operation = unsafe extern "C" fn(*mut GraalIsolateThread, *const c_char) -> *mut c_char;

#[link(name = "dynamicds")]
unsafe extern "C" {
    fn dd_create_isolate() -> *mut GraalIsolateThread;
    fn dd_get_isolate(thread: *mut GraalIsolateThread) -> *mut GraalIsolate;
    fn dd_attach_thread(isolate: *mut GraalIsolate) -> *mut GraalIsolateThread;
    fn dd_detach_thread(thread: *mut GraalIsolateThread) -> c_int;
    fn dd_open(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_close(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_execute(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_transaction(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_add_datasource(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_remove_datasource(thread: *mut GraalIsolateThread, request: *const c_char)
    -> *mut c_char;
    fn dd_list_datasources(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_tx_begin(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_tx_execute(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_tx_commit(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_tx_rollback(thread: *mut GraalIsolateThread, request: *const c_char) -> *mut c_char;
    fn dd_free(thread: *mut GraalIsolateThread, pointer: *mut c_char);
}

/// isolate 的地址。裸指针既不是 `Send` 也不是 `Sync`，存成 usize 才能放进全局变量。
static ISOLATE: OnceLock<usize> = OnceLock::new();

fn isolate() -> *mut GraalIsolate {
    // 创建 isolate 的线程随之 attach，之后它与其他线程一样经由 ATTACHED 取得自己的 isolate 线程
    *ISOLATE.get_or_init(|| unsafe { dd_get_isolate(dd_create_isolate()) } as usize)
        as *mut GraalIsolate
}

struct AttachedThread(*mut GraalIsolateThread);

impl Drop for AttachedThread {
    fn drop(&mut self) {
        unsafe { dd_detach_thread(self.0) };
    }
}

thread_local! {
    static ATTACHED: AttachedThread = AttachedThread(unsafe { dd_attach_thread(isolate()) });
}

fn call(operation: Operation, request: &Value) -> Result<Value, Error> {
    let request = CString::new(request.to_string()).expect("serialized JSON never contains NUL");
    let response = ATTACHED.with(|thread| unsafe {
        let pointer = operation(thread.0, request.as_ptr());
        let response = CStr::from_ptr(pointer).to_string_lossy().into_owned();
        dd_free(thread.0, pointer);
        response
    });
    let mut response: Value =
        serde_json::from_str(&response).expect("libdynamicds always answers JSON");
    if response["ok"] == true {
        Ok(response["data"].take())
    } else {
        Err(serde_json::from_value(response["error"].take()).expect("error has code and message"))
    }
}

async fn call_blocking(operation: Operation, request: Value) -> Result<Value, Error> {
    tokio::task::spawn_blocking(move || call(operation, &request))
        .await
        .expect("libdynamicds call panicked")
}

/// 与 Java 侧 `ErrorCode` 一一对应。
#[derive(Debug, Clone, Copy, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ErrorCode {
    BadRequest,
    NotOpen,
    AlreadyOpen,
    DatasourceNotFound,
    UpdateCountMismatch,
    TransactionNotFound,
    ConstraintViolation,
    SqlError,
    #[serde(other)]
    Internal,
}

impl ErrorCode {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::BadRequest => "BAD_REQUEST",
            Self::NotOpen => "NOT_OPEN",
            Self::AlreadyOpen => "ALREADY_OPEN",
            Self::DatasourceNotFound => "DATASOURCE_NOT_FOUND",
            Self::UpdateCountMismatch => "UPDATE_COUNT_MISMATCH",
            Self::TransactionNotFound => "TRANSACTION_NOT_FOUND",
            Self::ConstraintViolation => "CONSTRAINT_VIOLATION",
            Self::SqlError => "SQL_ERROR",
            Self::Internal => "INTERNAL",
        }
    }
}

/// libdynamicds 返回的 `{"ok":false,"error":{...}}`。
#[derive(Debug, Clone, Deserialize)]
pub struct Error {
    pub code: ErrorCode,
    pub message: String,
    #[serde(rename = "sqlState")]
    pub sql_state: Option<String>,
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}: {}", self.code.as_str(), self.message)
    }
}

impl std::error::Error for Error {}

/// 进程内只有一个 isolate，也只有一个打开的 DynamicRoutingDataSource，这个句柄可以随意复制。
#[derive(Debug, Clone, Copy)]
pub struct DynamicDataSource(());

impl DynamicDataSource {
    /// `dd_open`：config 的字段与 `spring.datasource.dynamic.*` 同名。
    pub async fn open(config: Value) -> Result<Self, Error> {
        call_blocking(dd_open, config).await.map(|_| Self(()))
    }

    /// `dd_close`：回滚未结束的会话式事务并关闭所有连接池。
    pub async fn close(self) -> Result<(), Error> {
        call_blocking(dd_close, json!({})).await.map(drop)
    }

    /// `dd_execute`：在 `ds` 指定的数据源或分组上执行一条语句，相当于调用一个 `@DS(ds)` 方法。
    pub async fn execute(&self, statement: Value) -> Result<Value, Error> {
        call_blocking(dd_execute, statement).await
    }

    /// `dd_transaction`：一次调用内把一批语句放进同一个本地事务。
    pub async fn transaction(&self, statements: Vec<Value>) -> Result<Value, Error> {
        call_blocking(dd_transaction, json!({ "statements": statements })).await
    }

    pub async fn add_datasource(&self, property: Value) -> Result<Value, Error> {
        call_blocking(dd_add_datasource, property).await
    }

    pub async fn remove_datasource(&self, name: &str) -> Result<Value, Error> {
        call_blocking(dd_remove_datasource, json!({ "name": name })).await
    }

    pub async fn list_datasources(&self) -> Result<Value, Error> {
        call_blocking(dd_list_datasources, json!({})).await
    }

    /// `dd_tx_begin`：开启跨多次调用的会话式事务。
    pub async fn begin(&self) -> Result<Transaction, Error> {
        let data = call_blocking(dd_tx_begin, json!({})).await?;
        Ok(Transaction {
            id: data["txId"]
                .as_str()
                .expect("dd_tx_begin returns txId")
                .to_owned(),
        })
    }

    /// 相当于 `@DSTransactional`：`work` 返回 `Ok` 时提交，返回 `Err` 时回滚。
    pub async fn transactional<T, E>(
        &self,
        work: impl AsyncFnOnce(&Transaction) -> Result<T, E>,
    ) -> Result<T, E>
    where
        E: From<Error>,
    {
        let transaction = self.begin().await?;
        match work(&transaction).await {
            Ok(value) => {
                transaction.commit().await?;
                Ok(value)
            }
            Err(error) => {
                // 回滚失败时还有 Java 侧的空闲超时兜底，这里保留业务错误本身
                let _ = transaction.rollback().await;
                Err(error)
            }
        }
    }
}

/// 会话式事务。语句可以从任意线程发出，Java 侧会交给该事务独占的线程执行。
#[derive(Debug)]
pub struct Transaction {
    id: String,
}

impl Transaction {
    pub fn id(&self) -> &str {
        &self.id
    }

    /// `dd_tx_execute`：请求与 [`DynamicDataSource::execute`] 相同。
    pub async fn execute(&self, mut statement: Value) -> Result<Value, Error> {
        statement["txId"] = json!(self.id);
        call_blocking(dd_tx_execute, statement).await
    }

    pub async fn commit(self) -> Result<(), Error> {
        call_blocking(dd_tx_commit, json!({ "txId": self.id }))
            .await
            .map(drop)
    }

    pub async fn rollback(self) -> Result<(), Error> {
        call_blocking(dd_tx_rollback, json!({ "txId": self.id }))
            .await
            .map(drop)
    }
}

/// 查询结果 `{"datasource","rows"}` 的第一行。
pub fn first_row(mut result: Value) -> Option<Value> {
    match result["rows"].take() {
        Value::Array(rows) => rows.into_iter().next(),
        _ => None,
    }
}
