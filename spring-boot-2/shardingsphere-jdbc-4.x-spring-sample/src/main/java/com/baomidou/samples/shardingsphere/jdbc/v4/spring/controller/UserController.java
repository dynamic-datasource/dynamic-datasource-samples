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
import java.util.List;
import java.util.Random;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Random RANDOM = new Random();
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** dynamic-datasource 的主库 */
    @GetMapping("master")
    public List<User> master() {
        return userService.selectUsersFromMaster();
    }

    /**
     * 注意：这里的“从库读”实际读的是分片主库，这是 ShardingSphere 4.1.1 的硬限制而非接线错误。 4.1.1 的每种规则各管各的 DataSource，且
     * ShardingRuleCondition 在配了 masterslave/encrypt 任一规则时直接拒绝建 shardingDataSource （"Have found
     * master-slave or encrypt rule in environment"），分片与读写分离只能二选一； 本示例保 t_order 分片， 故
     * selectUsersFromShardingSlave 只能指向 shardingDataSourceInShardingSphere。 同名方法在 5.x
     * （core-sample）里走单个混合规则数据源，SELECT 进 shardingslave0/1——写后经本端点读：4.x 可见、5.x 不可见。
     */
    @GetMapping("sharding_sphere")
    public List<User> shardingSlave() {
        return userService.selectUsersFromShardingSlave();
    }

    /**
     * dynamic-datasource 代理的 shardingSphere 的主库, 经过 2 次选择 第 1 次: master =>
     * shardingDataSourceInShardingSphere 第 2 次: shardingDataSourceInShardingSphere => master
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
     * dynamic-datasource 代理的 shardingSphere 的主库, 经过 2 次选择 第 1 次: master =>
     * shardingDataSourceInShardingSphere 第 2 次: shardingDataSourceInShardingSphere => master
     */
    @DeleteMapping("sharding_sphere/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return "成功删除用户" + id;
    }
}
