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
package com.baomidou.samples.druid.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.druid.entity.User;
import com.baomidou.samples.druid.mapper.UserMapper;
import com.baomidou.samples.druid.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<User> selectMasterUsers() {
        return userMapper.selectUsers(1);
    }

    @DS("slave")
    @Override
    public List<User> selectSlaveUsers() {
        return userMapper.selectUsers(1);
    }

    @Override
    public void addUser(User user) {
        userMapper.addUser(user.getName(), user.getAge());
    }

    @Override
    public void deleteUserById(Long id) {
        userMapper.deleteUserById(id);
    }
}