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

import com.baomidou.samples.atomikos.entity.Order;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 订单 Mapper，默认路由到 order 数据源（由 Service 上的 {@code @DS} 决定）。 */
public interface OrderMapper {

    @Insert("insert into orders (user_id, amount) values (#{userId}, #{amount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("select id, user_id, amount from orders where id = #{id}")
    Order selectById(@Param("id") Long id);

    @Select("select id, user_id, amount from orders")
    List<Order> selectAll();

    @Delete("delete from orders")
    int deleteAll();
}
