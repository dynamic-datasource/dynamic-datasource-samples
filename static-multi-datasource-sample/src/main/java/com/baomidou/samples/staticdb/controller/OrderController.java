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
package com.baomidou.samples.staticdb.controller;


import com.baomidou.samples.staticdb.entity.Order;
import com.baomidou.samples.staticdb.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class OrderController {

    private static final Random RANDOM = new Random();
    private final OrderService orderService;

    @GetMapping("master")
    public List<Order> masterUsers() {
        return orderService.selectMasterUsers();
    }


    @PostMapping
    public Order addUser() {
        Order order = new Order();
        order.setName("测试用户" + RANDOM.nextInt());
        order.setAge(RANDOM.nextInt(100));
        orderService.addUser(order);
        return order;
    }

    @DeleteMapping("{id}")
    public String deleteUser(@PathVariable Long id) {
        orderService.deleteUserById(id);
        return "成功删除用户" + id;
    }
}
