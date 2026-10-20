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
/** /users：读写分离与按请求头切换数据源，对应 jdbc-template-sample 与 spel-sample。 */
import { Hono } from 'hono';
import type { DynamicDataSource } from '../dynamic-datasource.ts';

export function users(dd: DynamicDataSource): Hono {
  const app = new Hono();

  /**
   * GET /users：默认读 slave 组（相当于 @DS("slave")），请求头 X-Datasource 相当于 spel-sample 的
   * @DS("#header.xxx")；响应头回显实际命中的成员。
   */
  app.get('/', async (c) => {
    const result = await dd.execute({
      ds: c.req.header('x-datasource') ?? 'slave',
      sql: 'select id, name, age from t_user order by id',
    });
    c.header('X-Datasource-Served', result.datasource);
    return c.json(result.rows);
  });

  /** POST /users：不指定数据源时写 primary，也就是 master。 */
  app.post('/', async (c) => {
    const user = await c.req.json<{ name: string; age?: number }>();
    const result = await dd.execute({
      sql: 'insert into t_user (name, age) values (?, ?)',
      params: [user.name, user.age ?? null],
      generatedKeys: ['id'],
    });
    return c.json(
      { id: result.generatedKeys![0].id, name: user.name, age: user.age ?? null, datasource: result.datasource },
      201,
    );
  });

  return app;
}
