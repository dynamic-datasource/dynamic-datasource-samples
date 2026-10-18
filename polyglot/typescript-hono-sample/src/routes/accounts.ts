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
/** /accounts，以及下单时扣减余额，对应 tx-local-sample 的 AccountService。 */
import { Hono } from 'hono';
import { firstRow, type DynamicDataSource, type Transaction } from '../dynamic-datasource.ts';
import { ApiError } from '../errors.ts';

export function accounts(dd: DynamicDataSource): Hono {
  const app = new Hono();

  /** GET /accounts/:id */
  app.get('/:id', async (c) => {
    const account = firstRow(
      await dd.execute({
        ds: 'account',
        sql: 'select id, balance from account where id = ?',
        params: [Number(c.req.param('id'))],
      }),
    );
    if (!account) {
      throw new ApiError(404, 'ACCOUNT_NOT_FOUND', '用户不存在');
    }
    return c.json(account);
  });

  return app;
}

/** 对应 AccountService#reduceBalance：检查并扣减余额。 */
export async function reduceBalance(tx: Transaction, userId: number, totalPrice: number): Promise<void> {
  const account = firstRow(
    await tx.execute({ ds: 'account', sql: 'select balance from account where id = ? for update', params: [userId] }),
  );
  if (!account) {
    throw new ApiError(404, 'ACCOUNT_NOT_FOUND', '用户不存在');
  }
  if ((account.balance as number) < totalPrice) {
    throw new ApiError(409, 'INSUFFICIENT_BALANCE', '余额不足');
  }
  await tx.execute({
    ds: 'account',
    sql: 'update account set balance = balance - ? where id = ?',
    params: [totalPrice, userId],
  });
}
