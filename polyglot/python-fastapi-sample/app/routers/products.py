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

"""/products，以及下单时扣减库存，对应 tx-local-sample 的 ProductService。"""

from __future__ import annotations

from typing import Any

from fastapi import APIRouter

from app.database import dd
from app.dynamic_datasource import Transaction, first_row
from app.errors import ApiError

router = APIRouter(tags=["products"])


@router.get("/products/{product_id}")
def get_product(product_id: int) -> dict[str, Any]:
    product = first_row(dd.execute("select id, price, stock from product where id = ?", [product_id], ds="product"))
    if product is None:
        raise ApiError(404, "PRODUCT_NOT_FOUND", "商品不存在")
    return product


def reduce_stock(tx: Transaction, product_id: int, amount: int) -> float:
    """对应 ``ProductService#reduceStock``：检查并扣减库存，返回总价。"""
    product = first_row(
        tx.execute("select price, stock from product where id = ? for update", [product_id], ds="product")
    )
    if product is None:
        raise ApiError(404, "PRODUCT_NOT_FOUND", "商品不存在")
    if product["stock"] < amount:
        raise ApiError(409, "INSUFFICIENT_STOCK", "库存不足")
    tx.execute("update product set stock = stock - ? where id = ?", [amount, product_id], ds="product")
    return product["price"] * amount
