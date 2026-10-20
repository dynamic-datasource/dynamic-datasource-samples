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
 * npm start / npm test 之前按需构建 libdynamicds：共享库已是最新时 build-libdynamicds.sh 什么也不做。
 * 用 DYNAMIC_DATASOURCE_LIB 指定了现成的共享库时不构建。
 */
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

if (!process.env.DYNAMIC_DATASOURCE_LIB) {
  const script = fileURLToPath(new URL('../../build-libdynamicds.sh', import.meta.url));
  process.exitCode = spawnSync('sh', [script], { stdio: 'inherit' }).status ?? 1;
}
