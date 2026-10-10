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
package com.baomidou.samples.seata.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.samples.seata.dto.PlaceOrderRequest;
import com.baomidou.samples.seata.entity.Account;
import com.baomidou.samples.seata.entity.Order;
import com.baomidou.samples.seata.entity.Product;
import com.baomidou.samples.seata.mapper.AccountMapper;
import com.baomidou.samples.seata.mapper.OrderMapper;
import com.baomidou.samples.seata.mapper.ProductMapper;
import com.baomidou.samples.seata.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@ActiveProfiles("postgresql")
class SeataSampleTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.6-trixie"))
                    .withInitScript("db/postgresql/init-schemas.sql");

    @Container
    static GenericContainer<?> seataServer =
            new GenericContainer<>(DockerImageName.parse("apache/seata-server:2.6.0"))
                    .withExposedPorts(8091)
                    .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.dynamic.datasource.order.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.dynamic.datasource.order.username", postgres::getUsername);
        registry.add("spring.datasource.dynamic.datasource.order.password", postgres::getPassword);
        registry.add("spring.datasource.dynamic.datasource.account.url", postgres::getJdbcUrl);
        registry.add(
                "spring.datasource.dynamic.datasource.account.username", postgres::getUsername);
        registry.add(
                "spring.datasource.dynamic.datasource.account.password", postgres::getPassword);
        registry.add("spring.datasource.dynamic.datasource.product.url", postgres::getJdbcUrl);
        registry.add(
                "spring.datasource.dynamic.datasource.product.username", postgres::getUsername);
        registry.add(
                "spring.datasource.dynamic.datasource.product.password", postgres::getPassword);
        registry.add(
                "seata.service.grouplist.default",
                () -> seataServer.getHost() + ":" + seataServer.getMappedPort(8091));
    }

    @Autowired private OrderService orderService;

    @Autowired private OrderMapper orderMapper;

    @Autowired private AccountMapper accountMapper;

    @Autowired private ProductMapper productMapper;

    @AfterEach
    void cleanUp() {
        DynamicDataSourceContextHolder.push("order");
        try {
            orderMapper.delete(null);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
        DynamicDataSourceContextHolder.push("product");
        try {
            Product product = productMapper.selectById(1);
            product.setStock(20);
            productMapper.updateById(product);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
        DynamicDataSourceContextHolder.push("account");
        try {
            Account account = accountMapper.selectById(1L);
            account.setBalance(50.0);
            accountMapper.updateById(account);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    @Test
    void placeOrderSuccess() {
        orderService.placeOrder(new PlaceOrderRequest(1L, 1L, 2));

        DynamicDataSourceContextHolder.push("order");
        try {
            assertThat(orderMapper.selectCount(null)).isEqualTo(1);
            Order order = orderMapper.selectList(null).get(0);
            assertThat(order.getStatus().name()).isEqualTo("SUCCESS");
            assertThat(order.getTotalPrice()).isEqualTo(20.0);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        DynamicDataSourceContextHolder.push("product");
        try {
            assertThat(productMapper.selectById(1).getStock()).isEqualTo(18);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        DynamicDataSourceContextHolder.push("account");
        try {
            assertThat(accountMapper.selectById(1L).getBalance()).isEqualTo(30.0);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    @Test
    void placeOrderRollbackOnInsufficientBalance() {
        // 总价 100 超过预置的余额 50，seata 全局事务必须回滚。
        assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(1L, 1L, 10)))
                .isInstanceOf(RuntimeException.class);

        DynamicDataSourceContextHolder.push("order");
        try {
            assertThat(orderMapper.selectCount(null)).isZero();
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        DynamicDataSourceContextHolder.push("product");
        try {
            assertThat(productMapper.selectById(1).getStock()).isEqualTo(20);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        DynamicDataSourceContextHolder.push("account");
        try {
            assertThat(accountMapper.selectById(1L).getBalance()).isEqualTo(50.0);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}
