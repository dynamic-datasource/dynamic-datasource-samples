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

"""/users：读写分离与按请求头切换数据源，对应 jdbc-template-sample 与 spel-sample。"""

from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Header, Response
from pydantic import BaseModel

from app.database import dd

router = APIRouter(tags=["users"])


@router.get("/users")
def list_users(response: Response, x_datasource: Annotated[str | None, Header()] = None) -> list[dict[str, Any]]:
    """默认读 slave 组（相当于 ``@DS("slave")``），请求头 X-Datasource 相当于 spel-sample 的 ``@DS("#header.xxx")``。"""
    result = dd.execute("select id, name, age from t_user order by id", ds=x_datasource or "slave")
    response.headers["X-Datasource-Served"] = result["datasource"]
    return result["rows"]


class NewUser(BaseModel):
    name: str
    age: int | None = None


@router.post("/users", status_code=201)
def create_user(user: NewUser) -> dict[str, Any]:
    """不指定数据源时写 primary，也就是 master。"""
    result = dd.execute("insert into t_user (name, age) values (?, ?)", [user.name, user.age], generated_keys=["id"])
    return {
        "id": result["generatedKeys"][0]["id"],
        "name": user.name,
        "age": user.age,
        "datasource": result["datasource"],
    }
