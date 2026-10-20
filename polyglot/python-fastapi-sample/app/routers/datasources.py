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

"""/datasources：运行时增删数据源，对应 add-remove-datasource-sample。"""

from __future__ import annotations

from typing import Any

from fastapi import APIRouter
from pydantic import BaseModel

from app.config import DB_DIR
from app.database import dd

router = APIRouter(tags=["datasources"])


@router.get("/datasources")
def list_datasources() -> dict[str, Any]:
    """当前的数据源与分组。"""
    return dd.list_datasources()


class NewDatasource(BaseModel):
    name: str
    url: str
    username: str | None = None
    password: str | None = None
    driver_class_name: str | None = None


@router.post("/datasources", status_code=201)
def add_datasource(datasource: NewDatasource) -> dict[str, Any]:
    """名称带下划线时自动入组（slave_3 → slave）。

    新从库沿用 t_user 的建表脚本：脚本路径由服务端决定，不接受客户端传入。
    """
    return dd.add_datasource(
        datasource.name,
        url=datasource.url,
        username=datasource.username,
        password=datasource.password,
        driverClassName=datasource.driver_class_name,
        init={"schema": f"file:{DB_DIR / 'user' / 'schema.sql'}"},
    )


@router.delete("/datasources/{name}")
def remove_datasource(name: str) -> dict[str, Any]:
    """删除数据源并关闭它的连接池，primary 不能删除。"""
    return dd.remove_datasource(name)
