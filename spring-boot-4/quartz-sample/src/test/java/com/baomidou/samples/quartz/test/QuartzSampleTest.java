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

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class QuartzSampleTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4.7-oraclelinux9"))
            .withDatabaseName("quartz");

    @org.springframework.test.context.DynamicPropertySource
    static void datasourceProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.dynamic.datasource.quartz.url",
                () -> mysql.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true");
        registry.add("spring.datasource.dynamic.datasource.quartz.username", mysql::getUsername);
        registry.add("spring.datasource.dynamic.datasource.quartz.password", mysql::getPassword);
    }

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DataSource dataSource;

    @Test
    void schedulerUsesJdbcStore() throws Exception {
        assertThat(scheduler.isStarted()).isTrue();
        assertThat(scheduler.getJobGroupNames()).contains("myJobGroup1");
        DynamicRoutingDataSource routingDataSource = (DynamicRoutingDataSource) dataSource;
        assertThat(routingDataSource.getDataSources().keySet()).contains("master", "quartz");
        Integer quartzTables = new JdbcTemplate(routingDataSource.getDataSource("quartz"))
                .queryForObject("select count(*) from QRTZ_JOB_DETAILS", Integer.class);
        assertThat(quartzTables).isGreaterThanOrEqualTo(1);
    }
}
