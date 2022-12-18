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
package com.baomidou.samples.shardingsphere.jdbc.v5.core.controller;

import com.baomidou.samples.shardingsphere.jdbc.v5.core.entity.TOrder;
import com.baomidou.samples.shardingsphere.jdbc.v5.core.service.TOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/t_order")
@RequiredArgsConstructor
public class TOrderController {
    private final TOrderService tOrderService;

    @GetMapping("/findAll")
    public List<TOrder> findAll() {
        return tOrderService.findAll();
    }

    @PostMapping("/addAll")
    public List<TOrder> addAll() {
        return tOrderService.addAll();
    }
}