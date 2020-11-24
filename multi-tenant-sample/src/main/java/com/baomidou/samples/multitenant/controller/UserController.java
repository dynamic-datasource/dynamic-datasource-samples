package com.baomidou.samples.jdbc.controller;


import com.baomidou.samples.jdbc.entity.User;
import com.baomidou.samples.jdbc.service.UserService;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
@DS("#header.tenantName")
/**
 *  #header header选择器,需要在request header中加入相应参数
 *  同理#session
 *  若方法需要制定主库则可在方法上加注解
 */
public class UserController {

    private static final Random RANDOM = new Random();
    private final UserService userService;

    /**
     * 参数制定数据源
     */
    @GetMapping(value = "/{tenantName}")
    @DS("#tenantName")
    public List<User> masterUsers(@PathVariable String tenantName) {
        return userService.selectMasterUsers();
    }

    @PostMapping
    public User addUser() {
        User user = new User();
        user.setName("测试用户" + RANDOM.nextInt());
        user.setAge(RANDOM.nextInt(100));
        userService.addUser(user);
        return user;
    }

    @DeleteMapping("{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return "成功删除用户" + id;
    }
}
