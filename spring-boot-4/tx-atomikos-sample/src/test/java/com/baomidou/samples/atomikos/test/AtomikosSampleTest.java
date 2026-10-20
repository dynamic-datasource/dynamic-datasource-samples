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
package com.baomidou.samples.atomikos.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import com.baomidou.samples.atomikos.dto.PlaceOrderRequest;
import com.baomidou.samples.atomikos.entity.Account;
import com.baomidou.samples.atomikos.entity.Order;
import com.baomidou.samples.atomikos.exception.InsufficientBalanceException;
import com.baomidou.samples.atomikos.service.AccountService;
import com.baomidou.samples.atomikos.service.OrderService;
import java.math.BigDecimal;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

@SpringBootTest
class AtomikosSampleTest {

    private static final Long USER_ID = 1L;

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");

    @Autowired private OrderService orderService;

    @Autowired private AccountService accountService;

    @Autowired private DynamicRoutingDataSource routingDataSource;

    @Autowired private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        orderService.deleteAll();
        accountService.reset(USER_ID, INITIAL_BALANCE);
    }

    @Test
    void datasourcesAreAtomikosXaAndJtaIsActive() {
        Map<String, DataSource> dataSources = routingDataSource.getDataSources();
        assertThat(dataSources).containsOnlyKeys("order", "account");
        // 路由数据源里的每个条目都被 dynamic-datasource 包装成 ItemDataSource，剥开后才是
        // Atomikos 的 XA 连接池。
        assertThat(dataSources.values())
                .allMatch(
                        ds ->
                                ds instanceof ItemDataSource
                                        && ((ItemDataSource) ds).getRealDataSource()
                                                instanceof AtomikosDataSourceBean);
        // classpath 上有 Atomikos 时 Spring Boot 自动装配 JtaTransactionManager，
        // 普通 @Transactional 即由 Atomikos 以两阶段提交接管。
        assertThat(transactionManager).isInstanceOf(JtaTransactionManager.class);
    }

    @Test
    void placeOrderCommitsAcrossTwoXaResources() {
        orderService.placeOrder(new PlaceOrderRequest(USER_ID, new BigDecimal("30.00")));

        assertThat(orderService.selectAll())
                .hasSize(1)
                .first()
                .satisfies(
                        order -> {
                            assertThat(order.getUserId()).isEqualTo(USER_ID);
                            assertThat(order.getAmount()).isEqualByComparingTo("30.00");
                        });
        Account account = accountService.selectByUserId(USER_ID);
        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void placeOrderRollsBackBothXaResourcesWhenBalanceInsufficient() {
        // 余额 100，扣减 500 必然失败：order 数据源里已插入的订单必须随 account 的回滚一起消失，
        // 这正是 XA 两阶段提交与 @DSTransactional 本地事务的本质区别。
        assertThatThrownBy(
                        () ->
                                orderService.placeOrder(
                                        new PlaceOrderRequest(USER_ID, new BigDecimal("500.00"))))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(orderService.selectAll()).isEmpty();
        Order order = orderService.selectById(1L);
        assertThat(order).isNull();
        Account account = accountService.selectByUserId(USER_ID);
        assertThat(account.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
    }
}
