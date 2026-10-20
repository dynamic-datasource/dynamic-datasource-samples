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
/**
 * HTTP 接口，与 rust-axum-sample、python-fastapi-sample 完全一致，约定见 ../README.md。
 *
 * 按 Hono 文档的 Best Practices 组织：每个资源一个子应用（routes/），这里用 app.route() 挂载它们。
 */
import { Hono } from 'hono';
import type { DynamicDataSource } from './dynamic-datasource.ts';
import { handleError } from './errors.ts';
import { accounts } from './routes/accounts.ts';
import { datasources } from './routes/datasources.ts';
import { orders } from './routes/orders.ts';
import { products } from './routes/products.ts';
import { users } from './routes/users.ts';

export function createApp(dd: DynamicDataSource, dbDir: string): Hono {
  const app = new Hono();
  app.onError(handleError);
  app.route('/users', users(dd));
  app.route('/datasources', datasources(dd, dbDir));
  app.route('/orders', orders(dd));
  app.route('/products', products(dd));
  app.route('/accounts', accounts(dd));
  return app;
}
