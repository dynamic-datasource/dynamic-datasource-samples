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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals(tOrderMapper.findAll().size(), 0);
    }

    @Test
    void whenRequestToAddByNameAndUserId() {
        int firstNumberOfAffectedRows = tOrderMapper.addByNameAndUserId("Bright", 114514L);
        int secondNumberOfAffectedRows = tOrderMapper.addByNameAndUserId("Jordan", 114515L);
        assertEquals(firstNumberOfAffectedRows + secondNumberOfAffectedRows, 2);
        assertEquals(tOrderMapper.findAll().size(), 2);
    }

    /**
     * TODO Tracked on <a href="https://github.com/apache/shardingsphere/issues/27955">When a logic database uses both `SHARDING` and `READWRITE_SPLITTING` features, CRUD operations on table throw `NoSuchTableException`</a>
     */
    @Test
    void whenRequestToAddByNameAndUserIdWithPrimaryKey() {
        List<TOrder> emptyState = tOrderMapper.findAll();
        assertEquals(emptyState.size(), 0);
        assertThrows(DataIntegrityViolationException.class, () -> {
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
        assertEquals(numberOfAffectedRows, 1);
        assertEquals(tOrderMapper.findAll().size(), 5);
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
        assertEquals(numberOfAffectedRows, 6);
        assertEquals(tOrderMapper.findAll().size(), 0);
    }
}