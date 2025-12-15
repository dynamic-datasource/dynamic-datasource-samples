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
package com.baomidou.samples.shardingsphere.jdbc.v5.core;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.samples.shardingsphere.jdbc.v5.core.entity.TOrder;
import com.baomidou.samples.shardingsphere.jdbc.v5.core.mapper.TOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MapperLayerTests {
    @Autowired
    TOrderMapper tOrderMapper;

    @BeforeEach
    void before() {
        DynamicDataSourceContextHolder.push("shardingSphere");
    }

    @AfterEach
    void after() {
        tOrderMapper.deleteAll();
        DynamicDataSourceContextHolder.clear();
    }

    @Test
    void whenRequestToFindAll() {
        assertEquals(0, tOrderMapper.findAll().size());
    }

    @Test
    void whenRequestToAddByNameAndUserId() {
        int firstNumberOfAffectedRows = tOrderMapper.addByNameAndUserId("Bright", 114514L);
        int secondNumberOfAffectedRows = tOrderMapper.addByNameAndUserId("Jordan", 114515L);
        assertEquals(2, firstNumberOfAffectedRows + secondNumberOfAffectedRows);
        assertEquals(2, tOrderMapper.findAll().size());
    }

    @Test
    void whenRequestToAddByNameAndUserIdWithPrimaryKey() {
        List<TOrder> emptyState = tOrderMapper.findAll();
        assertEquals(0, emptyState.size());
        assertDoesNotThrow(() -> {
            tOrderMapper.addAll(114514L, "Bright", 114514L);
            tOrderMapper.addAll(114515L, "Jordan", 114515L);
        });
    }

    @Test
    void whenRequestToDeleteById() {
        tOrderMapper.addByNameAndUserId("Bright", 114514L);
        tOrderMapper.addByNameAndUserId("Jordan", 114515L);
        tOrderMapper.addByNameAndUserId("Lemon", 114516L);
        tOrderMapper.addByNameAndUserId("Jack", 114517L);
        tOrderMapper.addByNameAndUserId("Michael", 114518L);
        tOrderMapper.addByNameAndUserId("Tony", 114519L);
        int numberOfAffectedRows = tOrderMapper.deleteById(114514L);
        assertEquals(1, numberOfAffectedRows);
        assertEquals(5, tOrderMapper.findAll().size());
    }

    @Test
    void whenRequestToDeleteAll() {
        tOrderMapper.addByNameAndUserId("Bright", 114514L);
        tOrderMapper.addByNameAndUserId("Jordan", 114515L);
        tOrderMapper.addByNameAndUserId("Lemon", 114516L);
        tOrderMapper.addByNameAndUserId("Jack", 114517L);
        tOrderMapper.addByNameAndUserId("Michael", 114518L);
        tOrderMapper.addByNameAndUserId("Tony", 114519L);
        int numberOfAffectedRows = tOrderMapper.deleteAll();
        assertEquals(6, numberOfAffectedRows);
        assertEquals(0, tOrderMapper.findAll().size());
    }
}
