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
package com.baomidou.samples.shardingsphere.jdbc.v5.core.mapper;

import com.baomidou.samples.shardingsphere.jdbc.v5.core.entity.TOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
@Component
public interface TOrderMapper {
    @Select("select * from t_order")
    List<TOrder> findAll();

    @Insert("insert into t_order (order_id,`name`,user_id) values (#{orderId},#{name},#{userId})")
    int addAll(@Param("orderId") Long orderId, @Param("name") String name, @Param("userId") Long userId);

    @Insert("insert into t_order (`name`,user_id) values (#{name},#{userId})")
    int addByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);

    @Delete("delete from t_order where user_id = #{userId}")
    int deleteById(Long userId);

    @Delete("delete from t_order")
    int deleteAll();
}