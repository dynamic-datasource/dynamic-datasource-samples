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

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class PostgreSqlDynamicDataSourceFacadeTest extends AbstractDynamicDataSourceFacadeTest {

    /** 与 compose.yaml 相同：initdb 阶段执行 init-databases.sql，为每个数据源建一个数据库。 */
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18.6-trixie"))
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    RESOURCES.resolve("db/postgresql/init-databases.sql")),
                            "/docker-entrypoint-initdb.d/init-databases.sql");

    @BeforeAll
    static void openPostgreSql() throws IOException {
        open(
                "postgresql",
                Map.of(
                        "POSTGRES_HOST", postgres.getHost(),
                        "POSTGRES_PORT", String.valueOf(port()),
                        "POSTGRES_USER", postgres.getUsername(),
                        "POSTGRES_PASSWORD", postgres.getPassword()));
    }

    @Override
    String slave3() {
        var json =
                """
                {
                  "name": "slave_3",
                  "url": "jdbc:postgresql://%s:%d/ds_slave_3",
                  "username": "%s",
                  "password": "%s",
                  "driverClassName": "org.postgresql.Driver",
                  "init": {"schema": "file:%s"}
                }
                """;
        return json.formatted(
                postgres.getHost(),
                port(),
                postgres.getUsername(),
                postgres.getPassword(),
                RESOURCES.resolve("db/user/schema.sql"));
    }

    private static int port() {
        return postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
    }
}
