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
package com.baomidou.samples.atomikos.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * H2 内存库的建表与预置数据。
 *
 * <p>本示例没有使用 {@code spring.datasource.dynamic.datasource.*.init.schema}：那条路径是在 XA
 * 连接池取出的连接上执行脚本，Atomikos 在事务管理器未运行时会话不提交，连接关闭时未提交的 DDL 会被回滚，表根本建不出来。这里用普通 DriverManager
 * 连接（auto-commit）先把内存库和表 建好，Atomikos 连接池随后连上的是同一个 JVM 内的同名内存库。
 *
 * <p>真实项目请直接连目标库执行 DDL，或交给 Flyway / Liquibase 管理。
 */
@Slf4j
@Component
public class H2SchemaInitializer implements ApplicationRunner {

    private static final String USERNAME = "sa";

    private static final String PASSWORD = "";

    @Override
    public void run(ApplicationArguments args) {
        initDatabase("order", "db/h2/schema-order.sql");
        initDatabase("account", "db/h2/schema-account.sql");
    }

    private void initDatabase(String database, String scriptLocation) {
        String url = "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, USERNAME, PASSWORD)) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new EncodedResource(new ClassPathResource(scriptLocation), "UTF-8"));
            try (ResultSet tables =
                    connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
                while (tables.next()) {
                    log.info(
                            "h2 database [{}] table [{}] ready",
                            database,
                            tables.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "failed to initialize h2 database [" + database + "]", e);
        }
    }
}
