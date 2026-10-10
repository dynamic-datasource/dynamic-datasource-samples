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
package com.baomidou.samples.pattern.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.samples.pattern.entity.User;
import com.baomidou.samples.pattern.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserServiceTest {

    @Autowired private UserService userService;

    @AfterEach
    void cleanUp() {
        userService.deleteAll();
    }

    @Test
    void namePatternRoutingCrud() {
        User user = new User();
        user.setName("Pattern");
        user.setAge(22);
        userService.addUser(user);

        assertThat(userService.selectAll()).isNotEmpty();

        User loaded = userService.selectAll().get(0);
        loaded.setName("PatternUpdated");
        userService.updateUser(loaded);
        assertThat(userService.selectAll().get(0).getName()).isEqualTo("PatternUpdated");

        userService.deleteUserById(loaded.getId().longValue());
        assertThat(userService.selectAll()).isEmpty();
    }
}
