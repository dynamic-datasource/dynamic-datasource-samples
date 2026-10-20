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
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Locale;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 在 {@code ds} 指定的数据源或分组上执行一条语句，效果等同于调用一个标了 {@code @DS(ds)} 的方法。
 *
 * <p>请求：{@code {"ds","sql","params","generatedKeys","expectUpdateCount"}}，除 {@code sql} 外都可省略；
 * {@code ds} 省略时走 primary。
 */
final class StatementExecutor {

    private StatementExecutor() {}

    static ObjectNode run(DynamicRoutingDataSource routing, String primary, JsonNode statement)
            throws SQLException {
        var sql = Json.required(statement, "sql");
        var ds = Json.text(statement, "ds");
        var generatedKeys = statement.path("generatedKeys");
        DynamicDataSourceContextHolder.push(ds);
        try {
            // 没有事务时，这与 DynamicRoutingDataSource#getConnection 的行为完全一致；先解析出目标数据源，
            // 是为了把分组里实际命中的成员告诉调用方。事务中必须走 getConnection，连接才会被事务接管。
            var target =
                    TransactionContext.getXID() == null ? routing.determineDataSource() : routing;
            var datasource =
                    switch (target) {
                        case ItemDataSource item -> item.getName();
                        default -> ds == null || ds.isEmpty() ? primary : ds;
                    };
            try (var connection = target.getConnection();
                    var ps = prepare(connection, sql, generatedKeys)) {
                bind(ps, statement.path("params"));
                var result = Json.MAPPER.createObjectNode().put("datasource", datasource);
                if (ps.execute()) {
                    try (var rs = ps.getResultSet()) {
                        return result.set("rows", rows(rs));
                    }
                }
                var updateCount = ps.getUpdateCount();
                var mismatch =
                        statement
                                .optional("expectUpdateCount")
                                .filter(expected -> !expected.isNull())
                                .map(JsonNode::asInt)
                                .filter(expected -> expected != updateCount);
                if (mismatch.isPresent()) {
                    throw new FacadeException(
                            ErrorCode.UPDATE_COUNT_MISMATCH,
                            "expected %d row(s) to be updated but was %d: %s"
                                    .formatted(mismatch.get(), updateCount, sql));
                }
                result.put("updateCount", updateCount);
                if (!generatedKeys.isEmpty()) {
                    try (var keys = ps.getGeneratedKeys()) {
                        result.set("generatedKeys", rows(keys));
                    }
                }
                return result;
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    private static PreparedStatement prepare(
            Connection connection, String sql, JsonNode generatedKeys) throws SQLException {
        if (generatedKeys.isEmpty()) {
            return connection.prepareStatement(sql);
        }
        var columns = generatedKeys.valueStream().map(JsonNode::asString).toArray(String[]::new);
        return connection.prepareStatement(sql, columns);
    }

    private static void bind(PreparedStatement ps, JsonNode params) throws SQLException {
        for (var i = 0; i < params.size(); i++) {
            var index = i + 1;
            switch (params.get(i)) {
                case NullNode _ -> ps.setNull(index, Types.NULL);
                case BooleanNode b -> ps.setBoolean(index, b.booleanValue());
                case NumericNode n when n.isIntegralNumber() -> ps.setLong(index, n.longValue());
                case NumericNode n -> ps.setDouble(index, n.doubleValue());
                case JsonNode other -> ps.setString(index, other.asString());
            }
        }
    }

    /** 列名统一小写（H2 返回大写，PostgreSQL 返回小写），值只转成 JSON 的基本类型。 */
    private static ArrayNode rows(ResultSet rs) throws SQLException {
        var rows = Json.MAPPER.createArrayNode();
        var metaData = rs.getMetaData();
        var columns = metaData.getColumnCount();
        while (rs.next()) {
            var row = rows.addObject();
            for (var c = 1; c <= columns; c++) {
                var label = metaData.getColumnLabel(c).toLowerCase(Locale.ROOT);
                switch (rs.getObject(c)) {
                    case null -> row.putNull(label);
                    case Boolean b -> row.put(label, b);
                    case BigDecimal d -> row.put(label, d);
                    case Double d -> row.put(label, d);
                    case Float f -> row.put(label, f);
                    case Number n -> row.put(label, n.longValue());
                    case Timestamp t -> row.put(label, t.toLocalDateTime().toString());
                    default -> row.put(label, rs.getString(c));
                }
            }
        }
        return rows;
    }
}
