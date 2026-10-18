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
 * libdynamicds（dynamic-datasource 的 GraalVM native 共享库）在 Node.js 里的封装。
 *
 * node:ffi 只有同步调用：主线程只负责创建 isolate，所有业务调用都交给固定大小的 worker 池。
 * 每个 worker 独占一个 OS 线程并在启动时 attach 一次，数据库调用因此不会阻塞事件循环。
 */
import { existsSync } from 'node:fs';
import { availableParallelism } from 'node:os';
import { Worker } from 'node:worker_threads';
import { load, type Operation } from './native.ts';
import type { WorkerData, WorkerRequest } from './worker.ts';

/** 一条语句；ds 为空时走 primary，相当于不标 @DS 的方法。 */
export interface Statement {
  ds?: string;
  sql: string;
  params?: unknown[];
  generatedKeys?: string[];
  expectUpdateCount?: number;
}

export type Row = Record<string, unknown>;

export interface ExecuteResult {
  /** 实际命中的数据源：分组请求时是被负载均衡选中的成员。 */
  datasource: string;
  rows?: Row[];
  updateCount?: number;
  generatedKeys?: Row[];
}

/** 查询结果的第一行。 */
export function firstRow(result: ExecuteResult): Row | undefined {
  return result.rows?.[0];
}

export interface Datasources {
  datasources: string[];
  groups: Record<string, string[]>;
}

/** libdynamicds 返回的 {"ok":false,"error":{...}}，code 与 Java 侧的 ErrorCode 一一对应。 */
export class DynamicDataSourceError extends Error {
  readonly code: string;
  readonly sqlState: string | undefined;

  constructor(error: { code: string; message: string; sqlState?: string }) {
    super(error.message);
    this.name = 'DynamicDataSourceError';
    this.code = error.code;
    this.sqlState = error.sqlState;
  }
}

interface Job {
  request: WorkerRequest;
  resolve: (response: string) => void;
  reject: (error: unknown) => void;
}

/** 每个 worker 同时只处理一个请求，其余请求排队等空闲的 worker。 */
class WorkerPool {
  readonly #workers: Worker[] = [];
  readonly #idle: Worker[] = [];
  readonly #queue: Job[] = [];
  readonly #running = new Map<Worker, Job>();

  constructor(data: WorkerData, size: number) {
    for (let i = 0; i < size; i++) {
      const worker = new Worker(new URL('./worker.ts', import.meta.url), { workerData: data });
      worker.on('message', (response: string) => this.#finish(worker, response));
      // 出错的 worker 随即退出：只让当前请求失败，不再给它派活
      worker.on('error', (error) => {
        this.#running.get(worker)?.reject(error);
        this.#running.delete(worker);
      });
      this.#workers.push(worker);
      this.#idle.push(worker);
    }
  }

  call(operation: Operation, request: unknown): Promise<string> {
    return new Promise((resolve, reject) => {
      this.#queue.push({ request: { operation, request: JSON.stringify(request) }, resolve, reject });
      this.#dispatch();
    });
  }

  async close(): Promise<void> {
    await Promise.all(
      this.#workers.map(
        (worker) =>
          new Promise<void>((resolve) => {
            worker.once('exit', () => resolve());
            worker.postMessage('close');
          }),
      ),
    );
  }

  #finish(worker: Worker, response: string): void {
    const job = this.#running.get(worker);
    this.#running.delete(worker);
    this.#idle.push(worker);
    job?.resolve(response);
    this.#dispatch();
  }

  #dispatch(): void {
    while (this.#idle.length > 0 && this.#queue.length > 0) {
      const worker = this.#idle.pop()!;
      const job = this.#queue.shift()!;
      this.#running.set(worker, job);
      worker.postMessage(job.request);
    }
  }
}

type Call = <T>(operation: Operation, request: unknown) => Promise<T>;

/** 会话式事务。语句可以由任意 worker 发出，Java 侧会交给该事务独占的线程执行。 */
export class Transaction {
  readonly id: string;
  readonly #call: Call;

  constructor(id: string, call: Call) {
    this.id = id;
    this.#call = call;
  }

  /** dd_tx_execute，请求与 DynamicDataSource#execute 相同。 */
  execute(statement: Statement): Promise<ExecuteResult> {
    return this.#call('dd_tx_execute', { ...statement, txId: this.id });
  }

  async commit(): Promise<void> {
    await this.#call('dd_tx_commit', { txId: this.id });
  }

  async rollback(): Promise<void> {
    await this.#call('dd_tx_rollback', { txId: this.id });
  }
}

/**
 * 进程内只有一个 isolate，多次 open 也共用它。主线程一直持有库的句柄：否则 close 之后 worker 全部退出，
 * 库会被 dlclose 卸载，而 isolate 里的 Java 线程（连接池等）仍在运行，再次 open 时进程直接崩溃。
 */
let shared: { native: ReturnType<typeof load>; isolate: bigint } | undefined;

/** 进程内唯一的 DynamicRoutingDataSource。 */
export class DynamicDataSource {
  readonly #pool: WorkerPool;

  private constructor(pool: WorkerPool) {
    this.#pool = pool;
  }

  /** 创建 isolate、启动 worker 池，然后 dd_open；config 的字段与 spring.datasource.dynamic.* 同名。 */
  static async open(config: object, options: { library: string; workers?: number }): Promise<DynamicDataSource> {
    if (!existsSync(options.library)) {
      throw new Error(
        `${options.library} 不存在：npm start / npm test 之前会用 GraalVM 构建它，请安装 GraalVM 25 并让 JAVA_HOME ` +
          '或 GRAALVM_HOME 指向它；或用 DYNAMIC_DATASOURCE_LIB 指向 libdynamicds.so',
      );
    }
    if (!shared) {
      const native = load(options.library);
      shared = { native, isolate: native.createIsolate() };
    }
    const workers = options.workers ?? Math.min(4, availableParallelism());
    const dd = new DynamicDataSource(new WorkerPool({ library: options.library, isolate: shared.isolate }, workers));
    await dd.#call('dd_open', config);
    return dd;
  }

  /** dd_close：回滚未结束的会话式事务、关闭所有连接池，再停掉 worker。 */
  async close(): Promise<void> {
    await this.#call('dd_close', {});
    await this.#pool.close();
  }

  /** dd_execute：在 ds 指定的数据源或分组上执行一条语句，相当于调用一个 @DS(ds) 方法。 */
  execute(statement: Statement): Promise<ExecuteResult> {
    return this.#call('dd_execute', statement);
  }

  /** dd_transaction：一次调用内把一批语句放进同一个本地事务。 */
  transaction(statements: Statement[]): Promise<{ xid: string; results: ExecuteResult[] }> {
    return this.#call('dd_transaction', { statements });
  }

  addDatasource(property: { name: string; url: string } & Record<string, unknown>): Promise<Datasources> {
    return this.#call('dd_add_datasource', property);
  }

  removeDatasource(name: string): Promise<Datasources> {
    return this.#call('dd_remove_datasource', { name });
  }

  listDatasources(): Promise<Datasources> {
    return this.#call('dd_list_datasources', {});
  }

  /** dd_tx_begin：开启跨多次调用的会话式事务。 */
  async begin(): Promise<Transaction> {
    const { txId } = await this.#call<{ txId: string }>('dd_tx_begin', {});
    return new Transaction(txId, this.#call);
  }

  /** 相当于 @DSTransactional：work 正常返回时提交，抛出异常时回滚。 */
  async transactional<T>(work: (tx: Transaction) => Promise<T>): Promise<T> {
    const tx = await this.begin();
    let result: T;
    try {
      result = await work(tx);
    } catch (error) {
      // 回滚失败时还有 Java 侧的空闲超时兜底，这里保留业务错误本身
      await tx.rollback().catch(() => {});
      throw error;
    }
    await tx.commit();
    return result;
  }

  readonly #call: Call = async <T>(operation: Operation, request: unknown): Promise<T> => {
    const response = JSON.parse(await this.#pool.call(operation, request));
    if (!response.ok) {
      throw new DynamicDataSourceError(response.error);
    }
    return response.data as T;
  };
}
