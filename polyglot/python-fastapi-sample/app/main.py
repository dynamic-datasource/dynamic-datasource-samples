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

"""在 FastAPI 里使用 dynamic-datasource，接口与 rust-axum-sample、typescript-hono-sample 一致，约定见 ../README.md。

按 FastAPI 文档的 Bigger Applications 组织：每个资源一个 APIRouter，放在 app/routers/ 下，这里负责组装。
路由都是同步 ``def``：FastAPI 把它们放进线程池执行，阻塞的 libdynamicds 调用不会卡住事件循环。
"""

from __future__ import annotations

import json
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI
from fastapi.concurrency import run_in_threadpool

from app.config import load_config
from app.database import dd
from app.dynamic_datasource import DynamicDataSourceError
from app.errors import ApiError, handle_api_error, handle_dynamic_datasource_error
from app.routers import accounts, datasources, orders, products, users


def create_app(config: dict[str, Any] | None = None) -> FastAPI:
    """启动时用 config（默认 load_config()）dd_open，关闭时 dd_close；测试里传入 Testcontainers 的连接信息。"""

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        described = await run_in_threadpool(dd.open, config if config is not None else load_config())
        print("dynamic-datasource is open:", json.dumps(described, ensure_ascii=False))
        yield
        await run_in_threadpool(dd.close)

    app = FastAPI(title="python-fastapi-sample", lifespan=lifespan)
    app.include_router(users.router)
    app.include_router(datasources.router)
    app.include_router(orders.router)
    app.include_router(products.router)
    app.include_router(accounts.router)
    app.add_exception_handler(ApiError, handle_api_error)
    app.add_exception_handler(DynamicDataSourceError, handle_dynamic_datasource_error)
    return app


app = create_app()
