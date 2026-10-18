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

"""/accounts，以及下单时扣减余额，对应 tx-local-sample 的 AccountService。"""

from __future__ import annotations

from typing import Any

from fastapi import APIRouter

from app.database import dd
from app.dynamic_datasource import Transaction, first_row
from app.errors import ApiError

router = APIRouter(tags=["accounts"])


@router.get("/accounts/{account_id}")
def get_account(account_id: int) -> dict[str, Any]:
    account = first_row(dd.execute("select id, balance from account where id = ?", [account_id], ds="account"))
    if account is None:
        raise ApiError(404, "ACCOUNT_NOT_FOUND", "用户不存在")
    return account


def reduce_balance(tx: Transaction, user_id: int, total_price: float) -> None:
    """对应 ``AccountService#reduceBalance``：检查并扣减余额。"""
    account = first_row(tx.execute("select balance from account where id = ? for update", [user_id], ds="account"))
    if account is None:
        raise ApiError(404, "ACCOUNT_NOT_FOUND", "用户不存在")
    if account["balance"] < total_price:
        raise ApiError(409, "INSUFFICIENT_BALANCE", "余额不足")
    tx.execute("update account set balance = balance - ? where id = ?", [total_price, user_id], ds="account")
