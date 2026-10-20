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
/** /orders：多数据源本地事务，对应 tx-local-sample 的 OrderController 与 OrderService。 */
import { Hono } from 'hono';
import { firstRow, type DynamicDataSource } from '../dynamic-datasource.ts';
import { ApiError } from '../errors.ts';
import { reduceBalance } from './accounts.ts';
import { reduceStock } from './products.ts';

interface PlaceOrder {
  user_id: number;
  product_id: number;
  amount: number;
}

export function orders(dd: DynamicDataSource): Hono {
  const app = new Hono();

  /**
   * POST /orders：与 OrderService#placeOrder 逐行对应，
   * 一个会话式事务跨 order、product、account 三个库，任何一步失败都整体回滚。
   */
  app.post('/', async (c) => {
    const order = checkAmount(await c.req.json<PlaceOrder>());
    const placed = await dd.transactional(async (tx) => {
      const inserted = await tx.execute({
        ds: 'order',
        sql: "insert into p_order (user_id, product_id, amount, status) values (?, ?, ?, 'INIT')",
        params: [order.user_id, order.product_id, order.amount],
        generatedKeys: ['id'],
      });
      const orderId = inserted.generatedKeys![0].id;
      const totalPrice = await reduceStock(tx, order.product_id, order.amount);
      await reduceBalance(tx, order.user_id, totalPrice);
      await tx.execute({
        ds: 'order',
        sql: "update p_order set status = 'SUCCESS', total_price = ? where id = ?",
        params: [totalPrice, orderId],
      });
      return { ...order, id: orderId, total_price: totalPrice, status: 'SUCCESS' };
    });
    return c.json(placed, 201);
  });

  /**
   * POST /orders/batch：先在事务外读单价，再把三条写语句一次交给 dd_transaction。
   * 库存、余额不足由 CHECK 约束兜底；单价在下单期间被改过则 expectUpdateCount 不满足，同样整体回滚。
   */
  app.post('/batch', async (c) => {
    const order = checkAmount(await c.req.json<PlaceOrder>());
    const product = firstRow(
      await dd.execute({ ds: 'product', sql: 'select price from product where id = ?', params: [order.product_id] }),
    );
    if (!product) {
      throw new ApiError(404, 'PRODUCT_NOT_FOUND', '商品不存在');
    }
    const price = product.price as number;
    const totalPrice = price * order.amount;
    const result = await dd.transaction([
      {
        ds: 'order',
        sql: "insert into p_order (user_id, product_id, amount, total_price, status) values (?, ?, ?, ?, 'SUCCESS')",
        params: [order.user_id, order.product_id, order.amount, totalPrice],
        generatedKeys: ['id'],
      },
      {
        ds: 'product',
        sql: 'update product set stock = stock - ? where id = ? and price = ?',
        params: [order.amount, order.product_id, price],
        expectUpdateCount: 1,
      },
      {
        ds: 'account',
        sql: 'update account set balance = balance - ? where id = ?',
        params: [totalPrice, order.user_id],
        expectUpdateCount: 1,
      },
    ]);
    const orderId = result.results[0].generatedKeys![0].id;
    return c.json({ ...order, id: orderId, total_price: totalPrice, status: 'SUCCESS' }, 201);
  });

  /** GET /orders */
  app.get('/', async (c) => {
    const result = await dd.execute({
      ds: 'order',
      sql: 'select id, user_id, product_id, amount, total_price, status from p_order order by id',
    });
    return c.json(result.rows);
  });

  return app;
}

function checkAmount(order: PlaceOrder): PlaceOrder {
  if (!(order.amount > 0)) {
    throw new ApiError(400, 'BAD_REQUEST', 'amount 必须大于 0');
  }
  return order;
}
