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
import { serve } from '@hono/node-server';
import { createApp } from './app.ts';
import { DB_DIR, LIBRARY, loadConfig } from './config.ts';
import { DynamicDataSource } from './dynamic-datasource.ts';

const dd = await DynamicDataSource.open(loadConfig(), { library: LIBRARY });
console.log('dynamic-datasource is open:', JSON.stringify(await dd.listDatasources()));

const port = Number(process.env.PORT ?? 8083);
const server = serve({ fetch: createApp(dd, DB_DIR).fetch, hostname: '0.0.0.0', port }, (info) =>
  console.log(`typescript-hono-sample listening on ${info.address}:${info.port}`),
);

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.once(signal, () => server.close(() => void dd.close()));
}
