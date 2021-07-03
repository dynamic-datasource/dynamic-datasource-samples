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
package com.baomidou.samples.beetl.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.samples.beetl.dao.UserDao;
import com.baomidou.samples.beetl.entity.User;
import com.baomidou.samples.beetl.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public List<User> selectMasterUsers() {
        return userDao.all();
    }

    @Override
    @DS("slave")
    public List<User> selectSlaveUsers() {
        return userDao.all();
    }

    @Override
    @Slave
    public List<User> selectSlaveAnnotationUsers() {
        return userDao.all();
    }

    @Override
    public void addUser(User user) {
        userDao.insert(user);
    }

    @Override
    public void deleteUserById(Long id) {
        userDao.deleteById(id);
    }
}
