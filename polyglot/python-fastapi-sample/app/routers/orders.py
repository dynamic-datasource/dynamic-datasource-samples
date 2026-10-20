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

"""/orders：多数据源本地事务，对应 tx-local-sample 的 OrderController 与 OrderService。"""

from __future__ import annotations

from typing import Any

from fastapi import APIRouter
from pydantic import BaseModel

from app.database import dd
from app.dynamic_datasource import first_row, statement
from app.errors import ApiError
from app.routers.accounts import reduce_balance
from app.routers.products import reduce_stock

router = APIRouter(tags=["orders"])


class PlaceOrder(BaseModel):
    user_id: int
    product_id: int
    amount: int


def check_amount(order: PlaceOrder) -> None:
    if order.amount <= 0:
        raise ApiError(400, "BAD_REQUEST", "amount 必须大于 0")


def placed(order_id: Any, order: PlaceOrder, total_price: float) -> dict[str, Any]:
    return {**order.model_dump(), "id": order_id, "total_price": total_price, "status": "SUCCESS"}


@router.post("/orders", status_code=201)
def place_order(order: PlaceOrder) -> dict[str, Any]:
    """与 ``OrderService#placeOrder`` 逐行对应。

    一个会话式事务跨 order、product、account 三个库，任何一步失败都整体回滚。
    """
    check_amount(order)
    with dd.transactional() as tx:
        inserted = tx.execute(
            "insert into p_order (user_id, product_id, amount, status) values (?, ?, ?, 'INIT')",
            [order.user_id, order.product_id, order.amount],
            ds="order",
            generated_keys=["id"],
        )
        order_id = inserted["generatedKeys"][0]["id"]
        total_price = reduce_stock(tx, order.product_id, order.amount)
        reduce_balance(tx, order.user_id, total_price)
        tx.execute(
            "update p_order set status = 'SUCCESS', total_price = ? where id = ?", [total_price, order_id], ds="order"
        )
    return placed(order_id, order, total_price)


@router.post("/orders/batch", status_code=201)
def place_order_batch(order: PlaceOrder) -> dict[str, Any]:
    """先在事务外读单价，再把三条写语句一次交给 dd_transaction。

    库存、余额不足由 CHECK 约束兜底；单价在下单期间被改过则 expectUpdateCount 不满足，同样整体回滚。
    """
    check_amount(order)
    product = first_row(dd.execute("select price from product where id = ?", [order.product_id], ds="product"))
    if product is None:
        raise ApiError(404, "PRODUCT_NOT_FOUND", "商品不存在")
    price = product["price"]
    total_price = price * order.amount
    result = dd.transaction(
        [
            statement(
                "insert into p_order (user_id, product_id, amount, total_price, status) values (?, ?, ?, ?, 'SUCCESS')",
                [order.user_id, order.product_id, order.amount, total_price],
                ds="order",
                generated_keys=["id"],
            ),
            statement(
                "update product set stock = stock - ? where id = ? and price = ?",
                [order.amount, order.product_id, price],
                ds="product",
                expect_update_count=1,
            ),
            statement(
                "update account set balance = balance - ? where id = ?",
                [total_price, order.user_id],
                ds="account",
                expect_update_count=1,
            ),
        ]
    )
    return placed(result["results"][0]["generatedKeys"][0]["id"], order, total_price)


@router.get("/orders")
def list_orders() -> list[dict[str, Any]]:
    sql = "select id, user_id, product_id, amount, total_price, status from p_order order by id"
    return dd.execute(sql, ds="order")["rows"]
