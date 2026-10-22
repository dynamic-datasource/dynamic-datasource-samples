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
package com.baomidou.samples.atomikos.controller;

import com.baomidou.samples.atomikos.dto.PlaceOrderRequest;
import com.baomidou.samples.atomikos.entity.Order;
import com.baomidou.samples.atomikos.service.OrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 下单入口，可直接 POST /order 体验跨 XA 数据源的下单流程。 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public String placeOrder(@RequestBody PlaceOrderRequest request) {
        orderService.placeOrder(request);
        return "ok";
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
        return orderService.selectById(id);
    }

    @GetMapping
    public List<Order> list() {
        return orderService.selectAll();
    }
}
