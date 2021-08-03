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
package com.baomidou.samples.mybatisplus3.controller;


import com.baomidou.samples.mybatisplus3.entity.User;
import com.baomidou.samples.mybatisplus3.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {

    private static final Random RANDOM = new Random();

    private final UserService userService;

    @GetMapping("master")
    public List<User> masterUsers() {
        return userService.selectMasterUsers();
    }

    @GetMapping("slave")
    public List<User> slaveUsers() {
        return userService.selectSlaveUsers();
    }

    @GetMapping("/lambdaMaster")
    public List<User> lambdaMasterUsers() {
        return userService.selectLambdaMasterUsers();
    }

    @GetMapping("/lambdaSlave")//fixme 似乎不生效
    public List<User> lambdaSlaveUsers() {
        return userService.selectLambdaSlaveUsers();
    }

    @GetMapping("slaveAnnotation")
    public List<User> slaveAnnotationUsers() {
        return userService.selectSlaveAnnotationUsers();
    }

    @PostMapping
    public User addUser() {
        User user = new User();
        user.setName("测试用户" + RANDOM.nextInt());
        user.setAge(RANDOM.nextInt(100));
        userService.addUser(user);
        return user;
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return "成功删除用户" + id;
    }
}
