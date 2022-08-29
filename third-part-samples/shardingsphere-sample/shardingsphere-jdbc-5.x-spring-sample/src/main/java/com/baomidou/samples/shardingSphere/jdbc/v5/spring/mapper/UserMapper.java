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
package com.baomidou.samples.shardingSphere.jdbc.v5.spring.mapper;

import com.baomidou.samples.shardingSphere.jdbc.v5.spring.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {
    @Select("select * from t_user")
    List<User> selectUsers();

    @Insert("insert into t_user (name,age) values (#{name},#{age})")
    boolean addUser(@Param("name") String name, @Param("age") Integer age);

    @Delete("delete from t_user where id = #{id}")
    void deleteUserById(Long id);
}
