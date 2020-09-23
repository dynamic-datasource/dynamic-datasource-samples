package com.baomidou.samples.spel.controller;

import com.baomidou.samples.spel.entity.User;
import com.baomidou.samples.spel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequestMapping("/users")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/session")
    public List<User> session(HttpServletRequest request) {
        request.getSession().setAttribute("tenantName", "tenant1");
        return userService.selectSpelBySession();
    }

    @GetMapping("/header")
    public String header() {
        userService.selectSpelByHeader();
        return "success";
    }

    @GetMapping("/spel1")
    public List<User> spel1() {
        return userService.selectSpelByKey("tenant1");
    }

    @GetMapping("/spel2")
    public List<User> spel2() {
        User user = new User();
        user.setTenantName("tenant2");
        return userService.selecSpelByTenant(user);
    }
}
