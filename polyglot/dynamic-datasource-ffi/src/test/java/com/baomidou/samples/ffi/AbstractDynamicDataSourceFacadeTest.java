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

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.samples.ffi.DynamicDataSourceFacade.Operation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** 同一组场景分别跑在 H2 与 PostgreSQL 上；门面是进程级单例，子类在 @BeforeAll 里 open。 */
abstract class AbstractDynamicDataSourceFacadeTest {

    /** 本模块自带的 config/ 与 db/；surefire 的工作目录是模块目录。 */
    static final Path RESOURCES = Path.of("src/test/resources").toAbsolutePath();

    static final int TX_IDLE_TIMEOUT_SECONDS = 2;

    /** 与三种语言的示例一样：读取 config/<profile>.json，再把 SQL 目录等放进 variables。 */
    static void open(String profile, Map<String, String> variables) throws IOException {
        var config =
                (ObjectNode)
                        Json.parse(
                                Files.readString(
                                        RESOURCES.resolve("config/%s.json".formatted(profile))));
        var configVariables =
                config.putObject("variables").put("DB_DIR", RESOURCES.resolve("db").toString());
        variables.forEach(configVariables::put);
        config.put("txIdleTimeoutSeconds", TX_IDLE_TIMEOUT_SECONDS);
        ok(DynamicDataSourceFacade::open, config.toString());
    }

    @AfterAll
    static void close() {
        ok(DynamicDataSourceFacade::close, "{}");
    }

    /** 动态添加演示用的 slave_3，H2 与 PostgreSQL 的连接信息不同。 */
    abstract String slave3();

    @BeforeEach
    void resetOrderData() {
        execute("order", "delete from p_order");
        execute("product", "update product set stock = 20 where id = 1");
        execute("account", "update account set balance = 50 where id = 1");
    }

    @Test
    void readsFromSlaveGroupMembersInTurn() {
        var served = new HashSet<String>();
        for (var i = 0; i < 4; i++) {
            var data = execute("slave", "select name from t_user order by id");
            var datasource = data.path("datasource").asString();
            assertThat(data.path("rows").get(0).path("name").asString())
                    .isEqualTo("%s-张三".formatted(datasource));
            served.add(datasource);
        }
        assertThat(served).containsExactlyInAnyOrder("slave_1", "slave_2");
    }

    @Test
    void strictModeRejectsUnknownDatasource() {
        assertThat(
                        errorCode(
                                DynamicDataSourceFacade::execute,
                                statement("nope", "select 1").toString()))
                .isEqualTo("DATASOURCE_NOT_FOUND");
    }

    @Test
    void writesGoToPrimaryAndReturnGeneratedKeys() {
        var data =
                ok(
                        DynamicDataSourceFacade::execute,
                        """
                        {
                          "sql": "insert into t_user (name, age) values (?, ?)",
                          "params": ["李四", 20],
                          "generatedKeys": ["id"]
                        }
                        """);
        assertThat(data.path("datasource").asString()).isEqualTo("master");
        assertThat(data.path("updateCount").asInt()).isEqualTo(1);
        var id = data.path("generatedKeys").get(0).path("id").asLong();
        assertThat(rows("master", "select name from t_user where id = ?", id))
                .extracting(row -> row.path("name").asString())
                .containsExactly("李四");
        assertThat(rows("slave_1", "select name from t_user where name = ?", "李四")).isEmpty();
    }

    @Test
    void addsDatasourceToGroupAndRemovesIt() {
        var added = ok(DynamicDataSourceFacade::addDataSource, slave3());
        assertThat(added.path("groups").path("slave"))
                .extracting(JsonNode::asString)
                .containsExactly("slave_1", "slave_2", "slave_3");
        var served = new HashSet<String>();
        for (var i = 0; i < 6; i++) {
            served.add(
                    execute("slave", "select count(*) c from t_user")
                            .path("datasource")
                            .asString());
        }
        assertThat(served).containsExactlyInAnyOrder("slave_1", "slave_2", "slave_3");

        var removed = ok(DynamicDataSourceFacade::removeDataSource, request("name", "slave_3"));
        assertThat(removed.path("groups").path("slave"))
                .extracting(JsonNode::asString)
                .containsExactly("slave_1", "slave_2");
        assertThat(errorCode(DynamicDataSourceFacade::removeDataSource, request("name", "master")))
                .isEqualTo("BAD_REQUEST");
        assertThat(errorCode(DynamicDataSourceFacade::removeDataSource, request("name", "slave_3")))
                .isEqualTo("DATASOURCE_NOT_FOUND");
    }

    @Test
    void batchTransactionCommitsAcrossDatasources() {
        var data = ok(DynamicDataSourceFacade::transaction, placeOrderBatch(1, 1));
        assertThat(data.path("xid").asString()).isNotBlank();
        assertThat(data.path("results"))
                .extracting(result -> result.path("datasource").asString())
                .containsExactly("order", "product", "account");
        assertThat(orderCount()).isEqualTo(1);
        assertThat(stock()).isEqualTo(19);
        assertThat(balance()).isEqualTo(40.0);
    }

    @Test
    void batchTransactionRollsBackEveryDatasourceOnConstraintViolation() {
        // 总价 60 超过余额 50：account 的 CHECK 约束失败，已经执行的 order、product 语句一起回滚
        var error = error(DynamicDataSourceFacade::transaction, placeOrderBatch(1, 6));
        assertThat(error.path("code").asString()).isEqualTo("CONSTRAINT_VIOLATION");
        assertThat(error.path("sqlState").asString()).startsWith("23");
        assertThat(orderCount()).isZero();
        assertThat(stock()).isEqualTo(20);
        assertThat(balance()).isEqualTo(50.0);
    }

    @Test
    void batchTransactionRollsBackOnUnexpectedUpdateCount() {
        assertThat(errorCode(DynamicDataSourceFacade::transaction, placeOrderBatch(999, 1)))
                .isEqualTo("UPDATE_COUNT_MISMATCH");
        assertThat(orderCount()).isZero();
        assertThat(stock()).isEqualTo(20);
    }

    @Test
    void sessionTransactionSpansCallerThreads() throws Exception {
        // 模拟宿主语言的线程池：begin、每条语句与 commit 都可能落在不同的线程上
        try (var callers = Executors.newFixedThreadPool(3)) {
            var txId = callers.submit(AbstractDynamicDataSourceFacadeTest::begin).get();
            var select =
                    statement(
                            "product",
                            "select price, stock from product where id = ? for update",
                            1);
            var product = callers.submit(() -> txExecute(txId, select)).get().path("rows").get(0);
            assertThat(product.path("stock").asLong()).isEqualTo(20);
            for (var statement : placeOrderStatements(1, 2)) {
                callers.submit(() -> txExecute(txId, statement)).get();
            }
            // 提交之前，事务外的连接看不到这些修改
            assertThat(orderCount()).isZero();
            callers.submit(
                            () ->
                                    ok(
                                            DynamicDataSourceFacade::commitTransaction,
                                            request("txId", txId)))
                    .get();
        }
        assertThat(orderCount()).isEqualTo(1);
        assertThat(stock()).isEqualTo(18);
        assertThat(balance()).isEqualTo(30.0);
    }

    @Test
    void sessionTransactionRollsBack() {
        var txId = begin();
        placeOrderStatements(1, 2).forEach(statement -> txExecute(txId, statement));
        ok(DynamicDataSourceFacade::rollbackTransaction, request("txId", txId));
        assertThat(orderCount()).isZero();
        assertThat(stock()).isEqualTo(20);
        assertThat(balance()).isEqualTo(50.0);
        assertThat(errorCode(DynamicDataSourceFacade::commitTransaction, request("txId", txId)))
                .isEqualTo("TRANSACTION_NOT_FOUND");
    }

    @Test
    void idleSessionTransactionIsRolledBack() throws InterruptedException {
        var txId = begin();
        txExecute(txId, placeOrderStatements(1, 1).getFirst());
        Thread.sleep(Duration.ofSeconds(TX_IDLE_TIMEOUT_SECONDS + 3));
        var select = statement("order", "select count(*) c from p_order").put("txId", txId);
        assertThat(errorCode(DynamicDataSourceFacade::executeInTransaction, select.toString()))
                .isEqualTo("TRANSACTION_NOT_FOUND");
        assertThat(orderCount()).isZero();
    }

    @Test
    void rejectsSecondOpen() {
        assertThat(errorCode(DynamicDataSourceFacade::open, "{}")).isEqualTo("ALREADY_OPEN");
    }

    /** 与 tx-local-sample 的下单流程相同：写订单、扣库存、扣余额，单价 10。 */
    static List<ObjectNode> placeOrderStatements(long userId, int amount) {
        var totalPrice = 10.0 * amount;
        var order =
                statement(
                        "order",
                        """
                        insert into p_order (user_id, product_id, amount, total_price, status)
                        values (?, ?, ?, ?, ?)
                        """,
                        userId,
                        1,
                        amount,
                        totalPrice,
                        "SUCCESS");
        var product =
                statement("product", "update product set stock = stock - ? where id = ?", amount, 1)
                        .put("expectUpdateCount", 1);
        var account =
                statement(
                                "account",
                                "update account set balance = balance - ? where id = ?",
                                totalPrice,
                                userId)
                        .put("expectUpdateCount", 1);
        return List.of(order, product, account);
    }

    static String placeOrderBatch(long userId, int amount) {
        var request = Json.MAPPER.createObjectNode();
        request.putArray("statements").addAll(placeOrderStatements(userId, amount));
        return request.toString();
    }

    static ObjectNode statement(String ds, String sql, Object... params) {
        var statement = Json.MAPPER.createObjectNode().put("ds", ds).put("sql", sql);
        var values = statement.putArray("params");
        for (var param : params) {
            values.add(Json.MAPPER.valueToTree(param));
        }
        return statement;
    }

    static JsonNode execute(String ds, String sql, Object... params) {
        return ok(DynamicDataSourceFacade::execute, statement(ds, sql, params).toString());
    }

    static JsonNode rows(String ds, String sql, Object... params) {
        return execute(ds, sql, params).path("rows");
    }

    static long orderCount() {
        return rows("order", "select count(*) c from p_order").get(0).path("c").asLong();
    }

    static long stock() {
        return rows("product", "select stock from product where id = 1")
                .get(0)
                .path("stock")
                .asLong();
    }

    static double balance() {
        return rows("account", "select balance from account where id = 1")
                .get(0)
                .path("balance")
                .asDouble();
    }

    static String begin() {
        return ok(DynamicDataSourceFacade::beginTransaction, "{}").path("txId").asString();
    }

    static JsonNode txExecute(String txId, ObjectNode statement) {
        return ok(
                DynamicDataSourceFacade::executeInTransaction,
                statement.deepCopy().put("txId", txId).toString());
    }

    /** 只有一个字段的请求，例如 {@code {"txId": "..."}}。 */
    static String request(String field, String value) {
        return Json.MAPPER.createObjectNode().put(field, value).toString();
    }

    static JsonNode ok(Operation operation, String request) {
        var response = Json.parse(DynamicDataSourceFacade.call(operation, request));
        assertThat(response.path("ok").asBoolean()).as(response.toString()).isTrue();
        return response.path("data");
    }

    static JsonNode error(Operation operation, String request) {
        var response = Json.parse(DynamicDataSourceFacade.call(operation, request));
        assertThat(response.path("ok").asBoolean()).as(response.toString()).isFalse();
        return response.path("error");
    }

    static String errorCode(Operation operation, String request) {
        return error(operation, request).path("code").asString();
    }
}
