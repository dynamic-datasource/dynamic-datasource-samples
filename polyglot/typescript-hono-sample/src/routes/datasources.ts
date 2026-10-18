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
/** /datasources：运行时增删数据源，对应 add-remove-datasource-sample。 */
import { join } from 'node:path';
import { Hono } from 'hono';
import type { DynamicDataSource } from '../dynamic-datasource.ts';

export function datasources(dd: DynamicDataSource, dbDir: string): Hono {
  const app = new Hono();

  /** GET /datasources：当前的数据源与分组。 */
  app.get('/', async (c) => c.json(await dd.listDatasources()));

  /**
   * POST /datasources：名称带下划线时自动入组（slave_3 → slave）。
   * 新从库沿用 t_user 的建表脚本：脚本路径由服务端决定，不接受客户端传入。
   */
  app.post('/', async (c) => {
    const datasource = await c.req.json<{
      name: string;
      url: string;
      username?: string;
      password?: string;
      driver_class_name?: string;
    }>();
    const result = await dd.addDatasource({
      name: datasource.name,
      url: datasource.url,
      username: datasource.username,
      password: datasource.password,
      driverClassName: datasource.driver_class_name,
      init: { schema: `file:${join(dbDir, 'user', 'schema.sql')}` },
    });
    return c.json(result, 201);
  });

  /** DELETE /datasources/:name：删除数据源并关闭它的连接池，primary 不能删除。 */
  app.delete('/:name', async (c) => c.json(await dd.removeDatasource(c.req.param('name'))));

  return app;
}
