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
 * worker 线程：每个 worker 独占一个 OS 线程，启动时 attach 到 isolate 一次，
 * 之后在本线程里同步调用 libdynamicds，把结果发回主线程。
 */
import { parentPort, workerData } from 'node:worker_threads';
import { load, type Operation } from './native.ts';

export interface WorkerData {
  library: string;
  isolate: bigint;
}

export interface WorkerRequest {
  operation: Operation;
  request: string;
}

const { library, isolate } = workerData as WorkerData;
const native = load(library);
const thread = native.attach(isolate);
const port = parentPort!;

port.on('message', (message: WorkerRequest | 'close') => {
  if (message === 'close') {
    native.detach(thread);
    port.close();
    return;
  }
  port.postMessage(native.call(thread, message.operation, message.request));
});
