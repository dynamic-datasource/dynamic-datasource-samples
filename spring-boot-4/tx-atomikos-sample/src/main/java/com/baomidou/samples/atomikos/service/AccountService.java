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

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.atomikos.entity.Account;
import com.baomidou.samples.atomikos.exception.InsufficientBalanceException;
import com.baomidou.samples.atomikos.mapper.AccountMapper;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 账户服务，所有方法都路由到 account 数据源。 */
@Slf4j
@Service
public class AccountService {

    private final AccountMapper accountMapper;

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @DS("account")
    public Account selectByUserId(Long userId) {
        return accountMapper.selectByUserId(userId);
    }

    /** 扣减余额；余额不足时抛出运行时异常，由调用方所在的 XA 事务整体回滚。 */
    @DS("account")
    public void deduct(Long userId, BigDecimal amount) {
        int updated = accountMapper.deduct(userId, amount);
        if (updated == 0) {
            throw new InsufficientBalanceException("用户 " + userId + " 余额不足，无法扣减 " + amount);
        }
        log.info("账户 {} 扣减 {} 成功", userId, amount);
    }

    /** 重置余额，仅供示例测试的用例之间清理数据使用。 */
    @DS("account")
    public void reset(Long userId, BigDecimal balance) {
        accountMapper.reset(userId, balance);
    }
}
