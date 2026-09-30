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
package com.baomidou.samples.mybatisplus3.test;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.samples.mybatisplus3.entity.User;
import com.baomidou.samples.mybatisplus3.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @AfterEach
    void cleanUp() {
        userService.remove(Wrappers.emptyWrapper());
    }

    @Test
    void addAndSelectMasterUsers() {
        User user = new User();
        user.setName("Tom");
        user.setAge(20);
        userService.addUser(user);
        assertThat(user.getId()).isNotNull();
        assertThat(userService.selectMasterUsers()).isNotEmpty();
        userService.deleteUserById(user.getId().longValue());
        assertThat(userService.selectMasterUsers()).isEmpty();
    }

    @Test
    void repositoryCrudMethods() {
        User user = new User();
        user.setName("Jerry");
        user.setAge(18);
        assertThat(userService.save(user)).isTrue();
        assertThat(user.getId()).isNotNull();

        User loaded = userService.getById(user.getId());
        assertThat(loaded.getName()).isEqualTo("Jerry");

        loaded.setAge(19);
        assertThat(userService.updateById(loaded)).isTrue();
        assertThat(userService.getById(user.getId()).getAge()).isEqualTo(19);

        assertThat(userService.count()).isEqualTo(1);
        assertThat(userService.list()).hasSize(1);

        assertThat(userService.removeById(user.getId())).isTrue();
        assertThat(userService.count()).isZero();
    }

    @Test
    void repositoryBatchMethods() {
        User first = new User();
        first.setName("Batch1");
        first.setAge(21);
        User second = new User();
        second.setName("Batch2");
        second.setAge(22);
        List<User> users = new ArrayList<>(Arrays.asList(first, second));
        assertThat(userService.saveBatch(users, 100)).isTrue();
        assertThat(userService.count()).isEqualTo(2);

        users.forEach(user -> user.setAge(30));
        assertThat(userService.updateBatchById(users, 100)).isTrue();
        assertThat(userService.list()).allMatch(user -> user.getAge() == 30);

        User third = new User();
        third.setName("Batch3");
        third.setAge(23);
        users.add(third);
        assertThat(userService.saveOrUpdateBatch(users, 100)).isTrue();
        assertThat(userService.count()).isEqualTo(3);

        assertThat(userService.removeByIds(Arrays.asList(first.getId(), second.getId(), third.getId()))).isTrue();
        assertThat(userService.count()).isZero();
    }

    @Test
    void slaveRoutingMethods() {
        User user = new User();
        user.setName("Slave");
        user.setAge(25);
        userService.addUser(user);
        try {
            assertThat(userService.selectSlaveUsers()).isNotEmpty();
            assertThat(userService.selectLambdaMasterUsers()).isNotEmpty();
            assertThat(userService.selectLambdaSlaveUsers()).isNotEmpty();
            assertThat(userService.selectSlaveAnnotationUsers()).isNotEmpty();
            assertThat(userService.lambdaQuery().list()).isNotEmpty();
        } finally {
            userService.deleteUserById(user.getId().longValue());
        }
    }
}
