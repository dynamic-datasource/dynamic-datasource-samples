package com.baomidou.samples.shiro.controller;

import com.baomidou.samples.shiro.entity.User;
import com.baomidou.samples.shiro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HelloController {

    private static final Map<String, String> TENANT_USER = new HashMap<>();

    static {
        TENANT_USER.put("zhangsan", "db1");
        TENANT_USER.put("lisi", "db2");
        TENANT_USER.put("wangwu", "db3");
    }

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "/system/login";
    }

    @GetMapping("/index")
    @RequiresAuthentication
    public String index(ModelMap modelMap) {
        List<User> users = userService.selectMasterUsers();
        modelMap.addAttribute("users", users);
        return "/system/index";
    }

    @PostMapping("/login")
    public void login(String username, String password) {
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
        subject.login(usernamePasswordToken);
        Session session = subject.getSession();
        session.setAttribute("tenant", TENANT_USER.get(username));
    }
}