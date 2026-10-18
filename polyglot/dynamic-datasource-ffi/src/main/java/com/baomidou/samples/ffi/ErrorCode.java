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
package com.baomidou.samples.ffi;

/** 返回给调用方的 {@code error.code}，各语言据此映射 HTTP 状态码，而不必认识 Java 异常类名。 */
public enum ErrorCode {
    /** 请求 JSON 格式错误或缺少必填字段，或者操作本身不被允许（例如删除 primary）。 */
    BAD_REQUEST,
    /** 还没有调用 {@code dd_open}，或者已经 {@code dd_close}。 */
    NOT_OPEN,
    /** 重复调用 {@code dd_open}。 */
    ALREADY_OPEN,
    /** strict 模式下找不到指定的数据源或分组。 */
    DATASOURCE_NOT_FOUND,
    /** 语句的 {@code expectUpdateCount} 与实际更新行数不符。 */
    UPDATE_COUNT_MISMATCH,
    /** 会话式事务不存在，已结束或因空闲超时被回滚。 */
    TRANSACTION_NOT_FOUND,
    /** 违反数据库约束（SQLState 23xxx），例如库存、余额的 CHECK 约束。 */
    CONSTRAINT_VIOLATION,
    /** 其他 SQL 错误。 */
    SQL_ERROR,
    /** 其他未预期的错误。 */
    INTERNAL
}
