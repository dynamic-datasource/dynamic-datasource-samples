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