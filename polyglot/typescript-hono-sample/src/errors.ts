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
/** 错误响应 {"code","message"}，三个示例的错误码一致；相当于 tx-local-sample 的 GlobalExceptionHandler。 */
import type { ErrorHandler } from 'hono';
import type { ContentfulStatusCode } from 'hono/utils/http-status';
import { DynamicDataSourceError } from './dynamic-datasource.ts';

/** dynamic-datasource 的错误码到 HTTP 状态码，其余一律 500。 */
const STATUSES: Record<string, ContentfulStatusCode> = {
  BAD_REQUEST: 400,
  DATASOURCE_NOT_FOUND: 404,
  CONSTRAINT_VIOLATION: 409,
  UPDATE_COUNT_MISMATCH: 409,
};

export class ApiError extends Error {
  readonly status: ContentfulStatusCode;
  readonly code: string;

  constructor(status: ContentfulStatusCode, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }

  static of(error: DynamicDataSourceError): ApiError {
    const message = error.message.toLowerCase();
    // 批量事务里库存、余额不足由 CHECK 约束兜底，按约束名还原成与会话式事务相同的业务错误
    if (error.code === 'CONSTRAINT_VIOLATION' && message.includes('ck_product_stock')) {
      return new ApiError(409, 'INSUFFICIENT_STOCK', '库存不足');
    }
    if (error.code === 'CONSTRAINT_VIOLATION' && message.includes('ck_account_balance')) {
      return new ApiError(409, 'INSUFFICIENT_BALANCE', '余额不足');
    }
    return new ApiError(STATUSES[error.code] ?? 500, error.code, error.message);
  }
}

/** 交给 app.onError：子应用里抛出的错误也由它统一转换成错误响应。 */
export const handleError: ErrorHandler = (error, c) => {
  if (error instanceof SyntaxError) {
    return c.json({ code: 'BAD_REQUEST', message: error.message }, 400);
  }
  const apiError = error instanceof DynamicDataSourceError ? ApiError.of(error) : error;
  if (apiError instanceof ApiError) {
    return c.json({ code: apiError.code, message: apiError.message }, apiError.status);
  }
  console.error(error);
  return c.json({ code: 'INTERNAL', message: String(error) }, 500);
};
