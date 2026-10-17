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
import java.util.List;
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

/**
 * Nacos 配置中心样例的测试。
 *
 * <p>按 Nacos 官方文档的用法，Spring Boot 4 通过 {@code com.alibaba.cloud:spring-alibaba-nacos-config} 与
 * {@code spring.config.import} 接入，测试服务端使用同代的 Nacos 3.x。Nacos 3.x 的 HTTP 客户端 API 只读不写，发布配置需要走运维
 * API，且运维 API 自带独立鉴权开关，测试里一并关闭。
 */
@Testcontainers
@SpringBootTest
class NacosSampleTest {

    @Container static GenericContainer<?> nacos = createNacosContainer();

    /**
     * 创建 Nacos 服务端容器，并把 8848/9848/9849 固定发布到本机同名端口。
     *
     * <p>native image 会在构建期把 {@code NacosConfigDataResource}（含配置文件中默认的 127.0.0.1:8848 与默认 gRPC 偏移量
     * 1000）固化进镜像，运行期的配置拉取仍使用这些值，因此需要让构建期固化的地址在运行期可达。
     */
    private static GenericContainer<?> createNacosContainer() {
        GenericContainer<?> container =
                new GenericContainer<>(DockerImageName.parse("nacos/nacos-server:v3.2.4"))
                        .withEnv("MODE", "standalone")
                        .withEnv("NACOS_AUTH_ENABLE", "false")
                        .withEnv("NACOS_AUTH_ADMIN_ENABLE", "false")
                        .withEnv("NACOS_AUTH_CONSOLE_ENABLE", "false")
                        .withEnv("NACOS_AUTH_TOKEN", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
                        .withEnv("NACOS_AUTH_IDENTITY_KEY", "serverIdentity")
                        .withEnv("NACOS_AUTH_IDENTITY_VALUE", "security")
                        .waitingFor(
                                Wait.forLogMessage(".*Nacos Server API started successfully.*", 1)
                                        .withStartupTimeout(Duration.ofMinutes(5)));
        container.setPortBindings(List.of("8848:8848", "9848:9848", "9849:9849"));
        return container;
    }

    /** 把 Nacos 服务地址写入系统属性：{@code spring.config.import} 在环境准备的最早期求值， 地址只能用系统属性覆盖。 */
    @BeforeAll
    static void setUp() throws Exception {
        System.setProperty("spring.nacos.config.server-addr", "127.0.0.1:8848");
        publishConfig();
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("spring.nacos.config.server-addr");
    }

    /** 通过运维 API 发布数据源配置，并等待配置在服务端可读后再启动 Spring 上下文 （服务端内部同步需要一点时间）。 */
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
        String baseUrl = "http://127.0.0.1:8848";
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
