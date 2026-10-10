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
package com.baomidou.samples.nacos.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class NacosSampleTest {

    // 按 https://nacos.io/docs/latest/ecology/use-nacos-with-spring-boot3/ 的官方用法，
    // Spring Boot 4 通过 com.alibaba.cloud:spring-alibaba-nacos-config + spring.config.import 接入，
    // 测试服务端用同代的 Nacos 3.x。Nacos 3.x 的 HTTP 客户端 API 只读不写，
    // 发布配置要走运维 API，且运维 API 自带独立鉴权开关，测试里一并关闭。
    @Container
    static GenericContainer<?> nacos =
            new GenericContainer<>(DockerImageName.parse("nacos/nacos-server:v3.2.4"))
                    .withEnv("MODE", "standalone")
                    .withEnv("NACOS_AUTH_ENABLE", "false")
                    .withEnv("NACOS_AUTH_ADMIN_ENABLE", "false")
                    .withEnv("NACOS_AUTH_CONSOLE_ENABLE", "false")
                    .withEnv("NACOS_AUTH_TOKEN", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
                    .withEnv("NACOS_AUTH_IDENTITY_KEY", "serverIdentity")
                    .withEnv("NACOS_AUTH_IDENTITY_VALUE", "security")
                    .withExposedPorts(8848, 9848, 9849)
                    .waitingFor(
                            Wait.forLogMessage(".*Nacos Server API started successfully.*", 1)
                                    .withStartupTimeout(Duration.ofMinutes(5)));

    @BeforeAll
    static void setUp() throws Exception {
        // spring.config.import 在环境准备的最早期求值，地址只能用系统属性覆盖。
        System.setProperty(
                "spring.nacos.config.server-addr",
                nacos.getHost() + ":" + nacos.getMappedPort(8848));
        // 3.x 客户端按（http 端口 + 偏移量）计算服务端的 gRPC 端口；
        // testcontainers 的随机端口映射下默认的 1000 偏移不再成立，因此按实际映射端口动态计算偏移。
        int offset = nacos.getMappedPort(9848) - nacos.getMappedPort(8848);
        System.setProperty("nacos.server.grpc.port.offset", String.valueOf(offset));
        publishConfig();
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("spring.nacos.config.server-addr");
        System.clearProperty("nacos.server.grpc.port.offset");
    }

    static void publishConfig() throws Exception {
        String yaml =
                "spring:\n"
                        + "  datasource:\n"
                        + "    dynamic:\n"
                        + "      datasource:\n"
                        + "        master:\n"
                        + "          driver-class-name: org.h2.Driver\n"
                        + "          url: jdbc:h2:mem:nacos_master\n"
                        + "          username: sa\n"
                        + "          password: \"\"\n";
        String baseUrl = "http://" + nacos.getHost() + ":" + nacos.getMappedPort(8848);
        String body =
                "dataId=dynamic-datasource.yaml&groupName=DEFAULT_GROUP&type=yaml&content="
                        + URLEncoder.encode(yaml, StandardCharsets.UTF_8);
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(baseUrl + "/nacos/v3/admin/cs/config"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"data\":true");
        // 发布后服务端内部同步需要一点时间，先等到配置可读再启动 Spring 上下文。
        HttpRequest query =
                HttpRequest.newBuilder(
                                URI.create(
                                        baseUrl
                                                + "/nacos/v3/client/cs/config?dataId=dynamic-datasource.yaml&groupName=DEFAULT_GROUP"))
                        .GET()
                        .build();
        boolean visible = false;
        for (int i = 0; i < 60 && !visible; i++) {
            HttpResponse<String> queryResponse =
                    client.send(query, HttpResponse.BodyHandlers.ofString());
            visible =
                    queryResponse.statusCode() == 200
                            && queryResponse.body().contains("jdbc:h2:mem:nacos_master");
            if (!visible) {
                Thread.sleep(1000);
            }
        }
        assertThat(visible).isTrue();
    }

    @Autowired private DataSource dataSource;

    @Test
    void datasourceConfigLoadedFromNacos() {
        DynamicRoutingDataSource routingDataSource = (DynamicRoutingDataSource) dataSource;
        assertThat(routingDataSource.getDataSources().keySet()).contains("master");
    }
}
