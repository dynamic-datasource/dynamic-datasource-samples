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
 * libdynamicds 的 node:ffi 绑定，主线程与 worker 共用。node:ffi 只有同步调用，
 * 所以真正的业务调用都放在 worker 里执行，见 dynamic-datasource.ts。
 */
import { dlopen, toString } from 'node:ffi';

/** 业务函数的统一签名：char* dd_xxx(graal_isolatethread_t* thread, const char* request)。 */
const OPERATION = { arguments: ['pointer', 'string'], return: 'pointer' } as const;

export type Operation =
  | 'dd_open'
  | 'dd_close'
  | 'dd_execute'
  | 'dd_transaction'
  | 'dd_add_datasource'
  | 'dd_remove_datasource'
  | 'dd_list_datasources'
  | 'dd_tx_begin'
  | 'dd_tx_execute'
  | 'dd_tx_commit'
  | 'dd_tx_rollback';

export function load(path: string) {
  const { lib, functions } = dlopen(path, {
    dd_create_isolate: { arguments: [], return: 'pointer' },
    dd_get_isolate: { arguments: ['pointer'], return: 'pointer' },
    dd_attach_thread: { arguments: ['pointer'], return: 'pointer' },
    dd_detach_thread: { arguments: ['pointer'], return: 'int32' },
    dd_free: { arguments: ['pointer', 'pointer'], return: 'void' },
    dd_open: OPERATION,
    dd_close: OPERATION,
    dd_execute: OPERATION,
    dd_transaction: OPERATION,
    dd_add_datasource: OPERATION,
    dd_remove_datasource: OPERATION,
    dd_list_datasources: OPERATION,
    dd_tx_begin: OPERATION,
    dd_tx_execute: OPERATION,
    dd_tx_commit: OPERATION,
    dd_tx_rollback: OPERATION,
  });
  return {
    /** 库的句柄：只要有人持有它，库就不会被 dlclose 卸载。 */
    lib,
    /** 创建 isolate 并返回它的句柄；调用线程随之 attach。 */
    createIsolate(): bigint {
      return functions.dd_get_isolate(functions.dd_create_isolate());
    },
    /** 把当前 OS 线程 attach 到 isolate，返回本线程的 isolate 线程。 */
    attach(isolate: bigint): bigint {
      return functions.dd_attach_thread(isolate);
    },
    detach(thread: bigint): void {
      functions.dd_detach_thread(thread);
    },
    /** 同步调用一个业务函数：请求与返回值都是 JSON，返回的字符串用 dd_free 释放。 */
    call(thread: bigint, operation: Operation, request: string): string {
      const pointer = functions[operation](thread, request);
      try {
        return toString(pointer) ?? '';
      } finally {
        functions.dd_free(thread, pointer);
      }
    },
  };
}
