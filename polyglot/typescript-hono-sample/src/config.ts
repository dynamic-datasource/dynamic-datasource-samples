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
import { readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/** 存放本示例自带的 config/ 与 db/ 的目录：默认是本项目的根目录，也可以用 SAMPLE_DIR 指定。 */
export const SAMPLE_DIR = resolve(process.env.SAMPLE_DIR ?? fileURLToPath(new URL('..', import.meta.url)));

export const DB_DIR = join(SAMPLE_DIR, 'db');

/** 默认用 ../dynamic-datasource-ffi 构建出的共享库。 */
export const LIBRARY =
  process.env.DYNAMIC_DATASOURCE_LIB ??
  fileURLToPath(new URL('../../dynamic-datasource-ffi/target/libdynamicds.so', import.meta.url));

/** 读取 config/<profile>.json（默认取 DD_PROFILE，再默认 h2），并通过 variables 告诉 dd_open 建表脚本的位置。 */
export function loadConfig(
  profile = process.env.DD_PROFILE ?? 'h2',
  variables: Record<string, string> = {},
): Record<string, unknown> {
  const config = JSON.parse(readFileSync(join(SAMPLE_DIR, 'config', `${profile}.json`), 'utf8'));
  return { ...config, variables: { DB_DIR, ...variables } };
}
