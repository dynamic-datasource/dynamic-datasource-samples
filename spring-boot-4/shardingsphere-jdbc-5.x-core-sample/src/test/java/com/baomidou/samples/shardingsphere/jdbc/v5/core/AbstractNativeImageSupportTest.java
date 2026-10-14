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
package com.baomidou.samples.shardingsphere.jdbc.v5.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * GraalVM native image 下的测试准备。
 *
 * <p>{@code org.testcontainers:mysql} 的 {@code MySQLContainer} 在 native image 下无法把 classpath 目录
 * {@code mysql-default-conf} 挂载进容器，会把 {@code /etc/mysql/conf.d} 破坏成 普通文件导致 mysqld 启动失败，见 <a
 * href="https://github.com/testcontainers/testcontainers-java/issues/7954">#7954</a>。因此 native 测试改用
 * {@link GenericContainer} 启动普通 MySQL 容器，并在测试类初始化时通过 系统属性把配置文件切换到符合 native 限制的 {@code
 * config-native.yaml} 并指向该容器；JVM 测试继续使用 {@code jdbc:tc:} 数据源与 {@code config.yaml}，不执行任何额外逻辑。
 */
abstract class AbstractNativeImageSupportTest {

    private static final String MYSQL_IMAGE = "mysql:26.7.0-oraclelinux9";

    private static final String MYSQL_ROOT_PASSWORD = "test";

    private static final String SYSTEM_PROPERTY_PREFIX = "sample.shardingsphere.";

    static {
        if ("runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"))) {
            configureNativeImageDatasources();
        }
    }

    private static void configureNativeImageDatasources() {
        GenericContainer<?> mysql =
                new GenericContainer<>(DockerImageName.parse(MYSQL_IMAGE))
                        .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_PASSWORD)
                        .withEnv("MYSQL_DATABASE", "test")
                        .withExposedPorts(3306);
        mysql.start();
        String jdbcUrl =
                "jdbc:mysql://"
                        + mysql.getHost()
                        + ":"
                        + mysql.getMappedPort(3306)
                        + "/test?useSSL=false&allowPublicKeyRetrieval=true";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            awaitDatabaseReady(jdbcUrl);
            runSchemaScript(jdbcUrl);
        } catch (ReflectiveOperationException | SQLException | IOException exception) {
            throw new IllegalStateException(
                    "Failed to prepare MySQL container for native image test", exception);
        }
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "config", "config-native.yaml");
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "driver", "com.mysql.cj.jdbc.Driver");
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "username", "root");
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "password", MYSQL_ROOT_PASSWORD);
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "master-url", jdbcUrl);
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "slave0-url", jdbcUrl);
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "slave1-url", jdbcUrl);
    }

    private static void awaitDatabaseReady(String jdbcUrl) throws SQLException {
        long deadline = System.currentTimeMillis() + 60_000L;
        while (true) {
            try (Connection connection =
                    DriverManager.getConnection(jdbcUrl, "root", MYSQL_ROOT_PASSWORD)) {
                return;
            } catch (SQLException exception) {
                if (System.currentTimeMillis() > deadline) {
                    throw exception;
                }
                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while waiting for MySQL", interruptedException);
                }
            }
        }
    }

    private static void runSchemaScript(String jdbcUrl) throws SQLException, IOException {
        String script;
        try (InputStream input =
                AbstractNativeImageSupportTest.class
                        .getClassLoader()
                        .getResourceAsStream("db/schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("db/schema.sql not found on classpath");
            }
            script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection connection =
                        DriverManager.getConnection(jdbcUrl, "root", MYSQL_ROOT_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
    }
}
