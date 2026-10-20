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

"""libdynamicds（dynamic-datasource 的 GraalVM native 共享库）的 ctypes 封装。

GraalVM 要求调用入口的 OS 线程已经 attach 到 isolate：每个线程第一次调用时 attach，并缓存在
``threading.local`` 里。ctypes 调用期间会释放 GIL，FastAPI 线程池里的多个请求可以同时进入 Java 侧。
"""

from __future__ import annotations

import ctypes
import json
import os
import threading
from collections.abc import Iterator, Sequence
from contextlib import contextmanager
from typing import Any

# 业务函数的统一签名：char* dd_xxx(graal_isolatethread_t* thread, const char* request)
_OPERATIONS = (
    "dd_open",
    "dd_close",
    "dd_execute",
    "dd_transaction",
    "dd_add_datasource",
    "dd_remove_datasource",
    "dd_list_datasources",
    "dd_tx_begin",
    "dd_tx_execute",
    "dd_tx_commit",
    "dd_tx_rollback",
)


class DynamicDataSourceError(Exception):
    """libdynamicds 返回的 ``{"ok": false, "error": {...}}``，``code`` 与 Java 侧的 ErrorCode 一一对应。"""

    def __init__(self, error: dict[str, Any]) -> None:
        super().__init__(f"{error['code']}: {error['message']}")
        self.code: str = error["code"]
        self.message: str = error["message"]
        self.sql_state: str | None = error.get("sqlState")


class _Library:
    """加载共享库并创建 isolate；一个进程只需要一个实例。"""

    def __init__(self, path: str) -> None:
        lib = ctypes.CDLL(path)
        lib.dd_create_isolate.argtypes = []
        lib.dd_create_isolate.restype = ctypes.c_void_p
        lib.dd_get_isolate.argtypes = [ctypes.c_void_p]
        lib.dd_get_isolate.restype = ctypes.c_void_p
        lib.dd_attach_thread.argtypes = [ctypes.c_void_p]
        lib.dd_attach_thread.restype = ctypes.c_void_p
        for name in _OPERATIONS:
            function = getattr(lib, name)
            function.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
            # 声明成 c_void_p 而不是 c_char_p：要拿到原始指针才能交还给 dd_free
            function.restype = ctypes.c_void_p
        lib.dd_free.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        lib.dd_free.restype = None
        self._lib = lib
        self._local = threading.local()
        # 创建 isolate 的线程随之 attach
        self._local.thread = lib.dd_create_isolate()
        self._isolate = lib.dd_get_isolate(self._local.thread)

    def _thread(self) -> int:
        thread = getattr(self._local, "thread", None)
        if thread is None:
            thread = self._local.thread = self._lib.dd_attach_thread(self._isolate)
        return thread

    def call(self, operation: str, request: dict[str, Any]) -> Any:
        thread = self._thread()
        pointer = getattr(self._lib, operation)(thread, json.dumps(request, ensure_ascii=False).encode())
        try:
            response = json.loads(ctypes.string_at(pointer).decode())
        finally:
            self._lib.dd_free(thread, pointer)
        if not response["ok"]:
            raise DynamicDataSourceError(response["error"])
        return response["data"]


_library: _Library | None = None
_library_lock = threading.Lock()


def _load(path: str) -> _Library:
    global _library
    with _library_lock:
        if _library is None:
            _library = _Library(path)
        return _library


def statement(
    sql: str,
    params: Sequence[Any] = (),
    *,
    ds: str | None = None,
    generated_keys: Sequence[str] | None = None,
    expect_update_count: int | None = None,
) -> dict[str, Any]:
    """一条语句的请求；``ds`` 为空时走 primary，相当于不标 ``@DS`` 的方法。"""
    request: dict[str, Any] = {"sql": sql, "params": list(params)}
    if ds is not None:
        request["ds"] = ds
    if generated_keys:
        request["generatedKeys"] = list(generated_keys)
    if expect_update_count is not None:
        request["expectUpdateCount"] = expect_update_count
    return request


def first_row(result: dict[str, Any]) -> dict[str, Any] | None:
    """查询结果 ``{"datasource","rows"}`` 的第一行。"""
    rows = result["rows"]
    return rows[0] if rows else None


class Transaction:
    """会话式事务。语句可以从任意线程发出，Java 侧会交给该事务独占的线程执行。"""

    def __init__(self, library: _Library, tx_id: str) -> None:
        self._library = library
        self.id = tx_id

    def execute(self, sql: str, params: Sequence[Any] = (), **options: Any) -> dict[str, Any]:
        """dd_tx_execute，参数与 :meth:`DynamicDataSource.execute` 相同。"""
        return self._library.call("dd_tx_execute", {"txId": self.id, **statement(sql, params, **options)})

    def commit(self) -> None:
        self._library.call("dd_tx_commit", {"txId": self.id})

    def rollback(self) -> None:
        self._library.call("dd_tx_rollback", {"txId": self.id})


class DynamicDataSource:
    """进程内唯一的 DynamicRoutingDataSource；所有方法都是阻塞调用，适合在 FastAPI 的同步路由里使用。"""

    def __init__(self, library_path: str | os.PathLike[str]) -> None:
        self._library = _load(os.fspath(library_path))

    def open(self, config: dict[str, Any]) -> dict[str, Any]:
        """dd_open：config 的字段与 ``spring.datasource.dynamic.*`` 同名，返回当前的数据源与分组。"""
        return self._library.call("dd_open", config)

    def close(self) -> None:
        """dd_close：回滚未结束的会话式事务并关闭所有连接池。"""
        self._library.call("dd_close", {})

    def execute(self, sql: str, params: Sequence[Any] = (), **options: Any) -> dict[str, Any]:
        """dd_execute：在 ``ds`` 指定的数据源或分组上执行一条语句，相当于调用一个 ``@DS(ds)`` 方法。"""
        return self._library.call("dd_execute", statement(sql, params, **options))

    def transaction(self, statements: Sequence[dict[str, Any]]) -> dict[str, Any]:
        """dd_transaction：一次调用内把一批语句（用 :func:`statement` 构造）放进同一个本地事务。"""
        return self._library.call("dd_transaction", {"statements": list(statements)})

    def add_datasource(self, name: str, **property: Any) -> dict[str, Any]:
        return self._library.call("dd_add_datasource", {"name": name, **property})

    def remove_datasource(self, name: str) -> dict[str, Any]:
        return self._library.call("dd_remove_datasource", {"name": name})

    def list_datasources(self) -> dict[str, Any]:
        return self._library.call("dd_list_datasources", {})

    def begin(self) -> Transaction:
        """dd_tx_begin：开启跨多次调用的会话式事务。"""
        return Transaction(self._library, self._library.call("dd_tx_begin", {})["txId"])

    @contextmanager
    def transactional(self) -> Iterator[Transaction]:
        """相当于 ``@DSTransactional``：with 块正常结束时提交，抛出异常时回滚。"""
        transaction = self.begin()
        try:
            yield transaction
        except BaseException:
            try:
                transaction.rollback()
            except DynamicDataSourceError:
                # 回滚失败时还有 Java 侧的空闲超时兜底，这里保留业务异常本身
                pass
            raise
        transaction.commit()
