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
package com.baomidou.samples.spel.service;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.spel.entity.User;

import java.util.List;

public interface UserService {

    List<User> selectSpelBySession();

    List<User> selectSpelByHeader();

    List<User> selectSpelByKey(String tenantName);

    List<User> selecSpelByTenant(User user);

    @DS("#tenantName")
    String getGroupNameInSpELSelf(String tenantName);

    @DS("#user.tenantName")
    String getGroupNameInsideObjectSpEL(User user);
}
