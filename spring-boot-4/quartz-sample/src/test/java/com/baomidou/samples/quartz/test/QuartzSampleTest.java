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
package com.baomidou.samples.quartz.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class QuartzSampleTest {

    @Container
    static MySQLContainer<?> mysql =
            new NativeAwareMySQLContainer<>(DockerImageName.parse("mysql:26.7.0-oraclelinux9"))
                    .withDatabaseName("quartz");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", QuartzSampleTest::mysqlJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add(
                "spring.datasource.dynamic.datasource.quartz.url", QuartzSampleTest::mysqlJdbcUrl);
        registry.add("spring.datasource.dynamic.datasource.quartz.username", mysql::getUsername);
        registry.add("spring.datasource.dynamic.datasource.quartz.password", mysql::getPassword);
    }

    /**
     * 返回 MySQL 容器地址。
     *
     * <p>AOT 处理测试上下文时容器尚未启动，条件求值需要一个等价的 MySQL URL 占位；容器启动后（JVM 测试与 native 测试运行期）始终返回真实地址。
     */
    private static String mysqlJdbcUrl() {
        return mysql.isRunning()
                ? mysql.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true"
                : "jdbc:mysql://localhost:3306/quartz?useSSL=false&allowPublicKeyRetrieval=true";
    }

    @Autowired private Scheduler scheduler;

    @Autowired private DataSource dataSource;

    @Test
    void schedulerUsesJdbcStore() throws Exception {
        assertThat(scheduler.isStarted()).isTrue();
        assertThat(scheduler.getJobGroupNames()).contains("myJobGroup1");
        DynamicRoutingDataSource routingDataSource = (DynamicRoutingDataSource) dataSource;
        assertThat(routingDataSource.getDataSources().keySet()).contains("master", "quartz");
        Integer quartzTables =
                new JdbcTemplate(routingDataSource.getDataSource("quartz"))
                        .queryForObject("select count(*) from QRTZ_JOB_DETAILS", Integer.class);
        assertThat(quartzTables).isGreaterThanOrEqualTo(1);
    }

    /**
     * native image 下跳过 {@code mysql-default-conf} 目录的挂载：Testcontainers 无法把 classpath 目录 资源复制进容器，会把
     * {@code /etc/mysql/conf.d} 破坏成普通文件导致 mysqld 启动失败，见 <a
     * href="https://github.com/testcontainers/testcontainers-java/issues/7954">#7954</a>；JVM
     * 测试行为保持不变。
     */
    static class NativeAwareMySQLContainer<SELF extends NativeAwareMySQLContainer<SELF>>
            extends MySQLContainer<SELF> {

        NativeAwareMySQLContainer(DockerImageName dockerImageName) {
            super(dockerImageName);
        }

        @Override
        protected void optionallyMapResourceParameterAsVolume(
                String paramName, String containerPath, String resourceName, Integer fileMode) {
            if (System.getProperty("org.graalvm.nativeimage.imagecode") == null) {
                super.optionallyMapResourceParameterAsVolume(
                        paramName, containerPath, resourceName, fileMode);
            }
        }
    }
}
