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
package com.baomidou.samples.consul.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class ConsulSampleTest {

    @Container
    static ConsulContainer consul =
            new ConsulContainer(DockerImageName.parse("hashicorp/consul:1.15"));

    @BeforeAll
    static void setUp() throws Exception {
        String yaml =
                "spring:\n"
                        + "  datasource:\n"
                        + "    dynamic:\n"
                        + "      datasource:\n"
                        + "        master:\n"
                        + "          driver-class-name: org.h2.Driver\n"
                        + "          url: jdbc:h2:mem:consul_master\n"
                        + "          username: sa\n"
                        + "          password: \"\"\n";
        String baseUrl = "http://" + consul.getHost() + ":" + consul.getMappedPort(8500);
        HttpRequest request =
                HttpRequest.newBuilder(
                                URI.create(baseUrl + "/v1/kv/dynamic-datasource/application/data"))
                        .PUT(HttpRequest.BodyPublishers.ofString(yaml))
                        .build();
        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        // 这里用系统属性而不用 @DynamicPropertySource，这样 Spring Cloud bootstrap 上下文也能读到这些值。
        System.setProperty("spring.cloud.consul.host", consul.getHost());
        System.setProperty("spring.cloud.consul.port", String.valueOf(consul.getMappedPort(8500)));
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("spring.cloud.consul.host");
        System.clearProperty("spring.cloud.consul.port");
    }

    @Autowired private DataSource dataSource;

    @Test
    void datasourceConfigLoadedFromConsul() {
        DynamicRoutingDataSource routingDataSource = (DynamicRoutingDataSource) dataSource;
        assertThat(routingDataSource.getDataSources().keySet()).contains("master");
    }
}
