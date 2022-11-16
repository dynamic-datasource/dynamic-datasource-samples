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
package com.baomidou.samples.shardingsphere.jdbc.v5.core.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.shardingsphere.jdbc.v5.core.entity.User;
import com.baomidou.samples.shardingsphere.jdbc.v5.core.mapper.UserMapper;
import com.baomidou.samples.shardingsphere.jdbc.v5.core.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Override
    public List<User> selectUsersFromMaster() {
        return userMapper.selectUsers();
    }

    @Override
    @DS("shardingSphere")
    public List<User> selectUsersFromShardingSlave() {
        return userMapper.selectUsers();
    }

    @DS("shardingSphere")
    @Override
    public void addUser(User user) {
        userMapper.addUser(user.getName(), user.getAge());
    }

    @DS("shardingSphere")
    @Override
    public void deleteUserById(Long id) {
        userMapper.deleteUserById(id);
    }
}
