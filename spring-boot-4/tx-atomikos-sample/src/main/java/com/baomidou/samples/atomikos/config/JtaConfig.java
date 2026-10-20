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
package com.baomidou.samples.atomikos.config;

import com.atomikos.icatch.jta.UserTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * 装配 Atomikos 的 JTA 事务管理器。
 *
 * <p>Spring Boot 3 及以前会自动装配 Atomikos 的 {@code UserTransactionManager}，但 Boot 4 起 JTA 自动配置只剩 JNDI
 * 变体，需要自行声明；不声明时事务管理器会回退成 {@code JdbcTransactionManager}，事务绑定的是主数据源上的单条连接， {@code @Transactional}
 * 退化为单库本地事务，XA 两阶段提交根本不会发生。
 *
 * <p>Atomikos 的事务日志默认落在工作目录的 {@code transaction-logs} 下。
 */
@org.springframework.context.annotation.Configuration
public class JtaConfig {

    /**
     * Atomikos 的事务管理器，同时实现 {@code jakarta.transaction.TransactionManager} 与 {@code
     * jakarta.transaction.UserTransaction}。
     */
    @Bean(initMethod = "init", destroyMethod = "close")
    public UserTransactionManager userTransactionManager() {
        return new UserTransactionManager();
    }

    @Bean
    public JtaTransactionManager transactionManager(UserTransactionManager userTransactionManager) {
        return new JtaTransactionManager(userTransactionManager, userTransactionManager);
    }
}
