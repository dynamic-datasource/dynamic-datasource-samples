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

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.creator.hikaricp.HikariCpConfig;
import com.baomidou.dynamic.datasource.creator.hikaricp.HikariDataSourceCreator;
import com.baomidou.dynamic.datasource.exception.CannotFindDataSourceException;
import com.baomidou.dynamic.datasource.provider.YmlDynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.strategy.RandomDynamicDataSourceStrategy;
import com.baomidou.dynamic.datasource.tx.DsPropagation;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import com.baomidou.dynamic.datasource.tx.TransactionalExecutor;
import com.baomidou.dynamic.datasource.tx.TransactionalInfo;
import com.baomidou.dynamic.datasource.tx.TransactionalTemplate;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * dynamic-datasource 的门面，{@link NativeEntryPoints} 与 JVM 单测共用。
 *
 * <p>每个 {@code dd_*} 操作都是一个 {@link Operation}：请求与返回值都是 JSON，失败时直接抛异常。{@link #call} 把结果包成 {@code
 * {"ok":true,"data":...}}，把异常包成 {@code
 * {"ok":false,"error":{"code","type","message","sqlState"}}}，自身从不抛异常：异常不能穿过 C 边界。
 */
public final class DynamicDataSourceFacade {

    static {
        // 赶在第一个 slf4j-simple 日志器创建之前生效，宿主语言用环境变量就能调整 Java 侧的日志级别
        var level = System.getenv("DD_LOG_LEVEL");
        if (level != null && !level.isBlank()) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceFacade.class);

    private static final TransactionalTemplate TX_TEMPLATE = new TransactionalTemplate();

    /** 与 {@code @DSTransactional} 的默认值一致；留空的话 TransactionalTemplate 遇到异常也会提交。 */
    @SuppressWarnings("unchecked")
    private static final Class<? extends Throwable>[] ROLLBACK_FOR = new Class[] {Exception.class};

    private static volatile Opened opened;

    private DynamicDataSourceFacade() {}

    /** 一个 {@code dd_*} 操作。 */
    @FunctionalInterface
    public interface Operation {
        JsonNode apply(JsonNode request) throws Throwable;
    }

    /** 解析请求、执行操作并返回 JSON 响应。 */
    public static String call(Operation operation, String request) {
        var response = Json.MAPPER.createObjectNode();
        try {
            response.put("ok", true).set("data", operation.apply(Json.parse(request)));
        } catch (Throwable t) {
            response.put("ok", false).set("error", error(t));
        }
        return Json.MAPPER.writeValueAsString(response);
    }

    /** {@code dd_open}：手工组装 DynamicRoutingDataSource，相当于 Spring Boot starter 的自动配置。 */
    public static synchronized JsonNode open(JsonNode request) {
        if (opened != null) {
            throw new FacadeException(ErrorCode.ALREADY_OPEN, "dynamic-datasource is already open");
        }
        var config = DataSourceConfig.parse(request);
        var creator = new DefaultDataSourceCreator();
        // 无参构造会让全局 HikariCpConfig 为 null，ConfigMergeCreator 合并配置时随即 NPE，
        // 见 https://github.com/baomidou/dynamic-datasource/issues/821
        creator.setCreators(List.of(new HikariDataSourceCreator(new HikariCpConfig())));
        if (config.lazy() != null) {
            creator.setLazy(config.lazy());
        }
        var provider = new YmlDynamicDataSourceProvider(creator, config.datasources());
        var routing = new DynamicRoutingDataSource(List.of(provider));
        routing.setPrimary(config.primary());
        routing.setStrict(config.strict());
        if (config.randomStrategy()) {
            routing.setStrategy(RandomDynamicDataSourceStrategy.class);
        }
        try {
            routing.afterPropertiesSet();
        } catch (RuntimeException e) {
            // 宿主进程不一定随之退出，先关掉已经加入的连接池
            routing.destroy();
            throw e;
        }
        var sessions = new TransactionSessions(config.txIdleTimeout());
        opened = new Opened(routing, creator, config.primary(), sessions);
        return describe(routing);
    }

    /** {@code dd_close}：回滚未结束的会话式事务并关闭所有连接池，之后可以再次 {@code dd_open}。 */
    public static synchronized JsonNode close(JsonNode request) {
        var current = opened;
        opened = null;
        if (current != null) {
            current.sessions().close();
            current.routing().destroy();
        }
        return null;
    }

    /** {@code dd_execute}：在 {@code ds} 指定的数据源或分组上执行一条语句。 */
    public static JsonNode execute(JsonNode request) throws SQLException {
        var current = current();
        return StatementExecutor.run(current.routing(), current.primary(), request);
    }

    /** {@code dd_transaction}：在一次调用内把一批语句放进同一个本地事务，语义同 {@code @DSTransactional}。 */
    public static JsonNode transaction(JsonNode request) throws Throwable {
        // 先确认已经 dd_open，再校验请求
        final var current = current();
        var statements = request.path("statements");
        if (!statements.isArray() || statements.isEmpty()) {
            throw new FacadeException(
                    ErrorCode.BAD_REQUEST, "'statements' must be a non-empty array");
        }
        var info = new TransactionalInfo();
        info.setPropagation(DsPropagation.REQUIRED);
        info.setRollbackFor(ROLLBACK_FOR);
        Callable<JsonNode> batch =
                () -> {
                    var result = Json.MAPPER.createObjectNode();
                    result.put("xid", TransactionContext.getXID());
                    var results = result.putArray("results");
                    for (var statement : statements) {
                        results.add(
                                StatementExecutor.run(
                                        current.routing(), current.primary(), statement));
                    }
                    return result;
                };
        return (JsonNode) TX_TEMPLATE.execute(new Transactional(info, batch));
    }

    /** {@code dd_add_datasource}：对应 add-remove-datasource-sample，名称带下划线时自动加入分组。 */
    public static JsonNode addDataSource(JsonNode request) {
        var current = current();
        var name = Json.required(request, "name");
        var property = DataSourceConfig.toProperty(name, request);
        current.routing().addDataSource(name, current.creator().createDataSource(property));
        return describe(current.routing());
    }

    /** {@code dd_remove_datasource}：删除数据源并关闭其连接池，primary 不能删除。 */
    public static JsonNode removeDataSource(JsonNode request) {
        var current = current();
        var name = Json.required(request, "name");
        if (name.equals(current.primary())) {
            throw new FacadeException(
                    ErrorCode.BAD_REQUEST, "the primary datasource can not be removed");
        }
        if (!current.routing().getDataSources().containsKey(name)) {
            throw new FacadeException(
                    ErrorCode.DATASOURCE_NOT_FOUND,
                    "dynamic-datasource could not find a datasource named %s".formatted(name));
        }
        current.routing().removeDataSource(name);
        return describe(current.routing());
    }

    /** {@code dd_list_datasources}：当前的数据源与分组。 */
    public static JsonNode listDataSources(JsonNode request) {
        return describe(current().routing());
    }

    /** {@code dd_tx_begin}：开启会话式事务，返回 {@code {"txId"}}。 */
    public static JsonNode beginTransaction(JsonNode request) throws Exception {
        return Json.MAPPER.createObjectNode().put("txId", current().sessions().begin());
    }

    /** {@code dd_tx_execute}：在会话式事务中执行一条语句，请求在 {@code dd_execute} 的基础上多一个 {@code txId}。 */
    public static JsonNode executeInTransaction(JsonNode request) throws Exception {
        var current = current();
        return current.sessions()
                .run(
                        Json.required(request, "txId"),
                        () -> StatementExecutor.run(current.routing(), current.primary(), request));
    }

    /** {@code dd_tx_commit}：提交会话式事务涉及的所有数据源。 */
    public static JsonNode commitTransaction(JsonNode request) throws Exception {
        current().sessions().commit(Json.required(request, "txId"));
        return null;
    }

    /** {@code dd_tx_rollback}：回滚会话式事务涉及的所有数据源。 */
    public static JsonNode rollbackTransaction(JsonNode request) throws Exception {
        current().sessions().rollback(Json.required(request, "txId"));
        return null;
    }

    private static Opened current() {
        var current = opened;
        if (current == null) {
            throw new FacadeException(
                    ErrorCode.NOT_OPEN, "dynamic-datasource is not open, call dd_open first");
        }
        return current;
    }

    private static ObjectNode describe(DynamicRoutingDataSource routing) {
        var result = Json.MAPPER.createObjectNode();
        var datasources = result.putArray("datasources");
        routing.getDataSources().keySet().stream().sorted().forEach(datasources::add);
        var groups = result.putObject("groups");
        new TreeMap<>(routing.getGroupDataSources())
                .forEach(
                        (name, group) -> {
                            var members = groups.putArray(name);
                            group.getDataSourceMap().keySet().stream()
                                    .sorted()
                                    .forEach(members::add);
                        });
        return result;
    }

    private static ObjectNode error(Throwable t) {
        var sqlException =
                Stream.iterate(t, Objects::nonNull, Throwable::getCause)
                        .filter(SQLException.class::isInstance)
                        .map(SQLException.class::cast)
                        .findFirst();
        var code =
                switch (t) {
                    case FacadeException e -> e.code();
                    case CannotFindDataSourceException _ -> ErrorCode.DATASOURCE_NOT_FOUND;
                    case JacksonException _ -> ErrorCode.BAD_REQUEST;
                    default ->
                            sqlException
                                    .map(DynamicDataSourceFacade::classify)
                                    .orElse(ErrorCode.INTERNAL);
                };
        if (code == ErrorCode.INTERNAL) {
            log.error("dynamic-datasource ffi call failed", t);
        }
        var error =
                Json.MAPPER
                        .createObjectNode()
                        .put("code", code.name())
                        .put("type", t.getClass().getName())
                        .put("message", String.valueOf(t.getMessage()));
        sqlException
                .map(SQLException::getSQLState)
                .ifPresent(state -> error.put("sqlState", state));
        return error;
    }

    /** SQLState 23xxx 是违反数据库约束，例如库存、余额的 CHECK 约束。 */
    private static ErrorCode classify(SQLException e) {
        var sqlState = e.getSQLState();
        return sqlState != null && sqlState.startsWith("23")
                ? ErrorCode.CONSTRAINT_VIOLATION
                : ErrorCode.SQL_ERROR;
    }

    private record Opened(
            DynamicRoutingDataSource routing,
            DefaultDataSourceCreator creator,
            String primary,
            TransactionSessions sessions) {}

    /** {@link TransactionalExecutor} 有两个抽象方法，写不成 lambda。 */
    private record Transactional(TransactionalInfo info, Callable<JsonNode> body)
            implements TransactionalExecutor {

        @Override
        public Object execute() throws Exception {
            return body.call();
        }

        @Override
        public TransactionalInfo getTransactionInfo() {
            return info;
        }
    }
}
