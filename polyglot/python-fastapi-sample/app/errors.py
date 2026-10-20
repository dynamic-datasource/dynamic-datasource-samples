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

"""错误响应 ``{"code","message"}``，三个示例的错误码一致；相当于 tx-local-sample 的 GlobalExceptionHandler。"""

from __future__ import annotations

from fastapi import Request
from fastapi.responses import JSONResponse

from app.dynamic_datasource import DynamicDataSourceError

# dynamic-datasource 的错误码到 HTTP 状态码，其余一律 500
_STATUSES = {"BAD_REQUEST": 400, "DATASOURCE_NOT_FOUND": 404, "CONSTRAINT_VIOLATION": 409, "UPDATE_COUNT_MISMATCH": 409}


class ApiError(Exception):
    def __init__(self, status: int, code: str, message: str) -> None:
        super().__init__(message)
        self.status = status
        self.code = code
        self.message = message

    @classmethod
    def of(cls, error: DynamicDataSourceError) -> ApiError:
        message = error.message.lower()
        # 批量事务里库存、余额不足由 CHECK 约束兜底，按约束名还原成与会话式事务相同的业务错误
        if error.code == "CONSTRAINT_VIOLATION" and "ck_product_stock" in message:
            return cls(409, "INSUFFICIENT_STOCK", "库存不足")
        if error.code == "CONSTRAINT_VIOLATION" and "ck_account_balance" in message:
            return cls(409, "INSUFFICIENT_BALANCE", "余额不足")
        return cls(_STATUSES.get(error.code, 500), error.code, error.message)


async def handle_api_error(_: Request, error: ApiError) -> JSONResponse:
    return JSONResponse({"code": error.code, "message": error.message}, status_code=error.status)


async def handle_dynamic_datasource_error(request: Request, error: DynamicDataSourceError) -> JSONResponse:
    return await handle_api_error(request, ApiError.of(error))
