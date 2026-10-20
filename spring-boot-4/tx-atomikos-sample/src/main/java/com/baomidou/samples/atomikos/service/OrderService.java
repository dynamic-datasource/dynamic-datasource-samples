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
package com.baomidou.samples.atomikos.service;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.atomikos.dto.PlaceOrderRequest;
import com.baomidou.samples.atomikos.entity.Order;
import com.baomidou.samples.atomikos.mapper.OrderMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务：下单流程跨越 order 与 account 两个 XA 数据源。
 *
 * <p>这里用编程式 JTA（{@code UserTransaction}）而不是 Spring 的 {@code @Transactional}，原因是 MyBatis-Spring 在
 * Spring 事务同步激活时会把整个事务复用同一个 SqlSession（连同同一个连接）， 该连接在事务内第一条语句执行时才按当时的 {@code @DS} 确定数据源，之后的
 * {@code @DS} 切换全部 失效，跨库写入会集中打到第一个数据源上。编程式 JTA 不注册 Spring 事务同步，每次 mapper 调用 各自取连接并按当前 {@code @DS}
 * 路由，Atomikos 自动把连接登记进当前 JTA 事务， {@code commit} 时对两个 XA 资源做两阶段提交。
 *
 * <p>若业务只操作单库，仍然可以用 Spring 的 {@code @Transactional}（配合 JtaConfig 里的 JtaTransactionManager）。
 */
@Slf4j
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final AccountService accountService;
    private final UserTransactionManager userTransactionManager;

    public OrderService(
            OrderMapper orderMapper,
            AccountService accountService,
            UserTransactionManager userTransactionManager) {
        this.orderMapper = orderMapper;
        this.accountService = accountService;
        this.userTransactionManager = userTransactionManager;
    }

    /** 下单：先写 order 数据源，再扣 account 数据源的余额，任意一步失败两个库一起回滚。 */
    @DS("order")
    public void placeOrder(PlaceOrderRequest request) {
        log.info("=============ORDER START=================");
        try {
            userTransactionManager.begin();
            log.info("收到下单请求, 用户:{}, 金额:{}, JTA 事务已开始", request.getUserId(), request.getAmount());
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setAmount(request.getAmount());
            orderMapper.insert(order);
            log.info("订单已写入 order 数据源, id={}", order.getId());
            accountService.deduct(request.getUserId(), request.getAmount());
            userTransactionManager.commit();
            log.info("账户扣减完成，两个 XA 资源已统一提交");
        } catch (RuntimeException e) {
            rollbackSafely();
            throw e;
        } catch (Exception e) {
            rollbackSafely();
            throw new IllegalStateException("JTA 事务提交失败", e);
        }
        log.info("=============ORDER END=================");
    }

    private void rollbackSafely() {
        try {
            userTransactionManager.rollback();
            log.info("JTA 事务已回滚");
        } catch (Exception rollbackException) {
            log.error("JTA 事务回滚失败", rollbackException);
        }
    }

    @DS("order")
    public Order selectById(Long id) {
        return orderMapper.selectById(id);
    }

    @DS("order")
    public List<Order> selectAll() {
        return orderMapper.selectAll();
    }

    /** 清空订单，仅供示例测试的用例之间清理数据使用。 */
    @DS("order")
    public void deleteAll() {
        orderMapper.deleteAll();
    }
}
