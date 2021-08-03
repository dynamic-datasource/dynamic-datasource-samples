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
package com.baomidou.samples.staticdb.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.staticdb.entity.Order;
import com.baomidou.samples.staticdb.mapper.order.OrderMapper;
import com.baomidou.samples.staticdb.service.OrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Override
    public List<Order> selectMasterUsers() {
        return orderMapper.selectUsers();
    }

    @DS("slave")
    @Override
    public List<Order> selectSlaveUsers() {
        return orderMapper.selectUsers();
    }

    @Override
    public void addUser(Order order) {
        orderMapper.addUser(order.getName(), order.getAge());
    }

    @Override
    public void deleteUserById(Long id) {
        orderMapper.deleteUserById(id);
    }
}
