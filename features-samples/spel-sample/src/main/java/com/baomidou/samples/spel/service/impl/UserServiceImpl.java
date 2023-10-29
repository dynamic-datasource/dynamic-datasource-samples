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
package com.baomidou.samples.spel.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.samples.spel.entity.User;
import com.baomidou.samples.spel.mapper.UserMapper;
import com.baomidou.samples.spel.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@DS("slave")
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @DS("#session.tenantName")
    public List<User> selectSpelBySession() {
        return userMapper.selectUsers();
    }

    @Override
    @DS("#header.tenantName")
    public List<User> selectSpelByHeader() {
        return userMapper.selectUsers();
    }

    @Override
    @DS("#tenantName")
    public List<User> selectSpelByKey(String tenantName) {
        assert !tenantName.equals("tenant1") || DynamicDataSourceContextHolder.peek().equals("tenant1");
        return userMapper.selectUsers();
    }

    @Override
    @DS("#user.tenantName")
    public List<User> selecSpelByTenant(User user) {
        return userMapper.selectUsers();
    }

    @Override
    @DS("#tenantName")
    public String getGroupNameInSpELSelf(String tenantName) {
        return DynamicDataSourceContextHolder.peek();
    }

    @Override
    @DS("#user.tenantName")
    public String getGroupNameInsideObjectSpEL(User user) {
        return DynamicDataSourceContextHolder.peek();
    }
}