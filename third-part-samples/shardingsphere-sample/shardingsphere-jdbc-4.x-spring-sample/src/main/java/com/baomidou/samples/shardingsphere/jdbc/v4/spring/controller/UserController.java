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
package com.baomidou.samples.shardingsphere.jdbc.v4.spring.controller;

import com.baomidou.samples.shardingsphere.jdbc.v4.spring.entity.User;
import com.baomidou.samples.shardingsphere.jdbc.v4.spring.service.UserService;
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
     * TODO 这是一个在 ShardingSphere 4.1.1 上有趣的案例，仍需要修复。它展示了 ShardingSphere 5.x 与 ShardingSphere 4.x 的设计理念差异
     * dynamic-datasource 代理的 shardingSphere 的从库, 经过 3 次选择
     * 第 1 次: master => masterSlaveDataSourceInShardingSphere
     * 第 2 次: masterSlaveDataSourceInShardingSphere => shardingslave0 or shardingslave1 in baomidou_readwrite_data_sources
     * 第 3 次: shardingslave0 or shardingslave1 in baomidou_readwrite_data_sources => master
     */
    @GetMapping("sharding_sphere")
    public List<User> shardingSlave() {
        return userService.selectUsersFromShardingSlave();
    }

    /**
     * dynamic-datasource 代理的 shardingSphere 的主库, 经过 2 次选择
     * 第 1 次: master => shardingDataSourceInShardingSphere
     * 第 2 次: shardingDataSourceInShardingSphere => master
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
     * 第 1 次: master => shardingDataSourceInShardingSphere
     * 第 2 次: shardingDataSourceInShardingSphere => master
     */
    @DeleteMapping("sharding_sphere/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return "成功删除用户" + id;
    }
}
