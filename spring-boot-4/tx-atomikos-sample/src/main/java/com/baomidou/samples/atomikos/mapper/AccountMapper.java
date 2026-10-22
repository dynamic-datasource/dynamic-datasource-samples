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
package com.baomidou.samples.atomikos.mapper;

import com.baomidou.samples.atomikos.entity.Account;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 账户 Mapper，默认路由到 account 数据源（由 Service 上的 {@code @DS} 决定）。 */
public interface AccountMapper {

    @Select("select id, user_id, balance from account where user_id = #{userId}")
    Account selectByUserId(@Param("userId") Long userId);

    /** 扣减余额，余额不足时影响行数为 0，由调用方转换为异常触发整体回滚。 */
    @Update(
            "update account set balance = balance - #{amount} where user_id = #{userId} and balance >= #{amount}")
    int deduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 重置余额，仅供示例测试的用例之间清理数据使用。 */
    @Update("update account set balance = #{balance} where user_id = #{userId}")
    void reset(@Param("userId") Long userId, @Param("balance") BigDecimal balance);
}
