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
package com.baomidou.samples.shardingsphere.jdbc.v5.spring.controller;

import com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity.User;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Random RANDOM = new Random();
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * dynamic-datasource 的主库
     */
    @GetMapping("master")
    public List<User> master() {
        return userService.selectUsersFromMaster();
    }

    /**
     * dynamic-datasource 代理的 shardingSphere 的从库, 经过 3 次选择
     * 第 1 次: master => baomidou_readwrite_data_sources in shardingSphere
     * 第 2 次: baomidou_readwrite_data_sources in shardingSphere => shardingslave0 or shardingslave1 in shardingSphere
     * 第 3 次: shardingslave0 or shardingslave1 in shardingSphere => master
     */
    @GetMapping("sharding_sphere")
    public List<User> shardingSlave() {
        return userService.selectUsersFromShardingSlave();
    }

    /**
     * dynamic-datasource 代理的 shardingSphere 的主库, 经过 2 次选择
     * 第 1 次: master => shardingmaster in shardingSphere
     * 第 2 次: shardingmaster in shardingSphere => master
     */
    @PostMapping("sharding_sphere")
    public User addUser() {
        User user = new User();
        user.setName("测试用户" + RANDOM.nextInt());
        user.setAge(RANDOM.nextInt(100));
        userService.addUser(user);
        return user;
    }

    /**
     * dynamic-datasource 代理的 shardingSphere 的主库, 经过 2 次选择
     * 第 1 次: master => shardingmaster in shardingSphere
     * 第 2 次: shardingmaster in shardingSphere => master
     */
    @DeleteMapping("sharding_sphere/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return "成功删除用户" + id;
    }
}
