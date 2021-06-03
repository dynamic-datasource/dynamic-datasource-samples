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
//package com.baomidou.samples.shiro.config;
//
//import com.baomidou.samples.shiro.service.UserService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.shiro.authc.*;
//import org.apache.shiro.authc.credential.DefaultPasswordService;
//import org.apache.shiro.authc.credential.PasswordService;
//import org.apache.shiro.authz.AuthorizationInfo;
//import org.apache.shiro.authz.SimpleAuthorizationInfo;
//import org.apache.shiro.realm.AuthorizingRealm;
//import org.apache.shiro.subject.PrincipalCollection;
//import org.apache.shiro.util.ByteSource;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.HashSet;
//import java.util.Set;
//@Slf4j
//public class MyRealm extends AuthorizingRealm {
//
//    @Autowired
//    private UserService userService;
//
//
//    /**
//     * 认证
//     *
//     * @param token
//     * @return
//     * @throws AuthenticationException
//     */
//    @Override
//    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
//        PasswordService passwordService = new DefaultPasswordService();
//
//        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
//        // token中获取username
//        String username = token.getPrincipal().toString();
//        SysUser user = this.userService.selectUserByName(username);
//
//        if (null == user) {
//            throw new UnknownAccountException("未知账户");
//        }
//
//        // 盐值
//        ByteSource salt = ByteSource.Util.bytes(user.getUserId());
//
//        SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(user, user.getPassword(), salt, getName());
//        return authenticationInfo;
//    }
//
//    /**
//     * 授权
//     * 这个方法在访问需要权限的地方就会调用，比如说页面上又shiro标签时，方法上有@RequiresPermissions注释时等。
//     *
//     * @param principals
//     * @return
//     */
//    @Override
//    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
//        log.info("-----进入授权方法，授权开始------------------------------------------------");
//        SysUser user = (SysUser) principals.getPrimaryPrincipal();
//        Set<String> permissions = new HashSet<>();
//        permissions = userService.listPermissionByUserId(user.getUserId());
//        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
//        info.setStringPermissions(permissions);
//        return info;
//    }
//}