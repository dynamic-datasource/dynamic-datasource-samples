package com.baomidou.samples.ds.controller;

import com.baomidou.samples.ds.entity.User;
import com.baomidou.samples.ds.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/interceptor")
public class TestInterceptorController {

    @Autowired
    private UserService userService;

    @GetMapping("a")
    public List<User> test1() {
        return userService.selectUsers();
    }


    @GetMapping("b")
    public List<User> test2() {
        return userService.selectUsers();
    }

    @GetMapping("c")
    public List<User> test3() {
        return userService.selectUsers();
    }
}
