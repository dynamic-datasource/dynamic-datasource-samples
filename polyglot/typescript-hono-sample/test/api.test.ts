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
 * 与 rust-axum-sample、python-fastapi-sample 相同的一组场景，分别跑在 H2 与 Testcontainers 启动的 PostgreSQL 上。
 *
 * isolate 与 dd_open 都是进程级单例；同一文件里的测试按顺序执行，依次打开、关闭各自的 DynamicRoutingDataSource。
 */
import assert from 'node:assert/strict';
import { join } from 'node:path';
import { test } from 'node:test';
import { PostgreSqlContainer } from '@testcontainers/postgresql';
import { createApp } from '../src/app.ts';
import { DB_DIR, LIBRARY, loadConfig, SAMPLE_DIR } from '../src/config.ts';
import { DynamicDataSource } from '../src/dynamic-datasource.ts';

test('h2', async () => {
  await runScenarios(loadConfig('h2'), {
    name: 'slave_3',
    url: 'jdbc:h2:mem:ds_slave_3;DB_CLOSE_DELAY=-1',
    username: 'sa',
    password: '',
    driver_class_name: 'org.h2.Driver',
  });
});

test('postgresql', async () => {
  // 与 compose.yaml 一样，initdb 阶段执行 init-databases.sql，为每个数据源建一个数据库
  const postgres = await new PostgreSqlContainer('postgres:18.6-trixie')
    .withUsername('postgres')
    .withPassword('postgres')
    .withCopyFilesToContainer([
      {
        source: join(SAMPLE_DIR, 'db', 'postgresql', 'init-databases.sql'),
        target: '/docker-entrypoint-initdb.d/init-databases.sql',
      },
    ])
    .start();
  try {
    const host = postgres.getHost();
    const port = String(postgres.getPort());
    const variables = {
      POSTGRES_HOST: host,
      POSTGRES_PORT: port,
      POSTGRES_USER: 'postgres',
      POSTGRES_PASSWORD: 'postgres',
    };
    await runScenarios(loadConfig('postgresql', variables), {
      name: 'slave_3',
      url: `jdbc:postgresql://${host}:${port}/ds_slave_3`,
      username: 'postgres',
      password: 'postgres',
      driver_class_name: 'org.postgresql.Driver',
    });
  } finally {
    await postgres.stop();
  }
});

const order = (productId: number, amount: number) => ({ user_id: 1, product_id: productId, amount });

/** 动态添加的 slave_3 由调用方给出，因为它的连接信息随数据库不同。 */
async function runScenarios(config: Record<string, unknown>, slave3: Record<string, string>): Promise<void> {
  const dd = await DynamicDataSource.open(config, { library: LIBRARY });
  const app = createApp(dd, DB_DIR);
  const get = (path: string, datasource?: string) =>
    app.request(path, { headers: datasource ? { 'X-Datasource': datasource } : {} });
  const send = (method: string, path: string, body?: unknown) =>
    app.request(path, {
      method,
      body: body === undefined ? undefined : JSON.stringify(body),
      headers: { 'Content-Type': 'application/json' },
    });
  const names = async (response: Response) => ((await response.json()) as { name: string }[]).map((u) => u.name);
  const assertOrderState = async (orders: number, stock: number, balance: number) => {
    assert.equal(((await (await get('/orders')).json()) as unknown[]).length, orders);
    assert.equal(((await (await get('/products/1')).json()) as { stock: number }).stock, stock);
    assert.equal(((await (await get('/accounts/1')).json()) as { balance: number }).balance, balance);
  };

  try {
    // 读请求默认走 slave 组，两次请求轮询到两个成员
    const served = new Set<string>();
    for (let i = 0; i < 2; i++) {
      const response = await get('/users');
      assert.equal(response.status, 200);
      const datasource = response.headers.get('X-Datasource-Served')!;
      assert.equal(((await response.json()) as { name: string }[])[0].name, `${datasource}-张三`);
      served.add(datasource);
    }
    assert.deepEqual([...served].sort(), ['slave_1', 'slave_2']);

    // 请求头指定数据源；strict 模式下未知数据源直接报错
    let response = await get('/users', 'master');
    assert.equal(response.headers.get('X-Datasource-Served'), 'master');
    assert.equal((await names(response))[0], 'master-张三');
    response = await get('/users', 'nope');
    assert.equal(response.status, 404);
    assert.equal(((await response.json()) as { code: string }).code, 'DATASOURCE_NOT_FOUND');

    // 写请求走 primary
    response = await send('POST', '/users', { name: '李四', age: 20 });
    assert.equal(response.status, 201);
    const created = (await response.json()) as { id: unknown; datasource: string };
    assert.equal(created.datasource, 'master');
    assert.equal(typeof created.id, 'number');
    assert.ok((await names(await get('/users', 'master'))).includes('李四'));
    assert.ok(!(await names(await get('/users', 'slave_1'))).includes('李四'));

    // 运行时增删数据源，slave_3 自动进入 slave 组
    response = await send('POST', '/datasources', slave3);
    assert.equal(response.status, 201, await response.clone().text());
    assert.deepEqual(((await response.json()) as { groups: { slave: string[] } }).groups.slave, [
      'slave_1',
      'slave_2',
      'slave_3',
    ]);
    const servedAfterAdd = new Set<string>();
    for (let i = 0; i < 3; i++) {
      servedAfterAdd.add((await get('/users')).headers.get('X-Datasource-Served')!);
    }
    assert.equal(servedAfterAdd.size, 3);
    response = await send('DELETE', '/datasources/slave_3');
    assert.equal(response.status, 200);
    assert.deepEqual(((await response.json()) as { groups: { slave: string[] } }).groups.slave, ['slave_1', 'slave_2']);
    assert.equal((await send('DELETE', '/datasources/master')).status, 400);

    // 会话式事务下单：与 tx-local-sample 相同的流程，失败时三个库一起回滚
    response = await send('POST', '/orders', order(1, 1));
    assert.equal(response.status, 201, await response.clone().text());
    assert.equal(((await response.json()) as { total_price: number }).total_price, 10);
    await assertOrderState(1, 19, 40);
    response = await send('POST', '/orders', order(1, 6));
    assert.equal(response.status, 409);
    assert.equal(((await response.json()) as { code: string }).code, 'INSUFFICIENT_BALANCE');
    response = await send('POST', '/orders', order(1, 30));
    assert.equal(((await response.json()) as { code: string }).code, 'INSUFFICIENT_STOCK');
    assert.equal((await send('POST', '/orders', order(999, 1))).status, 404);
    await assertOrderState(1, 19, 40);

    // 批量事务下单：一次 dd_transaction，CHECK 约束兜底
    response = await send('POST', '/orders/batch', order(1, 1));
    assert.equal(response.status, 201, await response.clone().text());
    await assertOrderState(2, 18, 30);
    response = await send('POST', '/orders/batch', order(1, 6));
    assert.equal(response.status, 409);
    assert.equal(((await response.json()) as { code: string }).code, 'INSUFFICIENT_BALANCE');
    response = await send('POST', '/orders/batch', order(1, 30));
    assert.equal(((await response.json()) as { code: string }).code, 'INSUFFICIENT_STOCK');
    await assertOrderState(2, 18, 30);

    // 并发请求分给 worker 池里的多个线程，各自 attach 到 isolate
    const statuses = await Promise.all(Array.from({ length: 20 }, async () => (await get('/users')).status));
    assert.deepEqual(statuses, Array(20).fill(200));
  } finally {
    await dd.close();
  }
}
