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
/** /products，以及下单时扣减库存，对应 tx-local-sample 的 ProductService。 */
import { Hono } from 'hono';
import { firstRow, type DynamicDataSource, type Transaction } from '../dynamic-datasource.ts';
import { ApiError } from '../errors.ts';

export function products(dd: DynamicDataSource): Hono {
  const app = new Hono();

  /** GET /products/:id */
  app.get('/:id', async (c) => {
    const product = firstRow(
      await dd.execute({
        ds: 'product',
        sql: 'select id, price, stock from product where id = ?',
        params: [Number(c.req.param('id'))],
      }),
    );
    if (!product) {
      throw new ApiError(404, 'PRODUCT_NOT_FOUND', '商品不存在');
    }
    return c.json(product);
  });

  return app;
}

/** 对应 ProductService#reduceStock：检查并扣减库存，返回总价。 */
export async function reduceStock(tx: Transaction, productId: number, amount: number): Promise<number> {
  const product = firstRow(
    await tx.execute({
      ds: 'product',
      sql: 'select price, stock from product where id = ? for update',
      params: [productId],
    }),
  );
  if (!product) {
    throw new ApiError(404, 'PRODUCT_NOT_FOUND', '商品不存在');
  }
  if ((product.stock as number) < amount) {
    throw new ApiError(409, 'INSUFFICIENT_STOCK', '库存不足');
  }
  await tx.execute({
    ds: 'product',
    sql: 'update product set stock = stock - ? where id = ?',
    params: [amount, productId],
  });
  return (product.price as number) * amount;
}
