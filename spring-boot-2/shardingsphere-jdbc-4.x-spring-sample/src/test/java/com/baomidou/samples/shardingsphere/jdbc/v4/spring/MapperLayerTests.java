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
package com.baomidou.samples.shardingsphere.jdbc.v4.spring;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.samples.shardingsphere.jdbc.v4.spring.entity.TOrder;
import com.baomidou.samples.shardingsphere.jdbc.v4.spring.mapper.TOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TODO Unit tests for ShardingSphere 5.4.0 need to be synchronized.
 */
@SpringBootTest
@Disabled("TODO wait for com.baomidou:dynamic-datasource-spring-boot-starter:4.2.0 release")
public class MapperLayerTests {
    @Autowired
    TOrderMapper tOrderMapper;

    @BeforeEach
    void before() {
        DynamicDataSourceContextHolder.push("shardingDataSourceInShardingSphere");
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
        int firstNumberOfAffectedRows = tOrderMapper.addAll(101L, "Bright", 114514L);
        int secondNumberOfAffectedRows = tOrderMapper.addAll(102L, "Jordan", 114515L);
        assertEquals(2, firstNumberOfAffectedRows + secondNumberOfAffectedRows);
        assertEquals(2, tOrderMapper.findAll().size());
    }

    @Test
    void whenRequestToAddByNameAndUserIdWithPrimaryKey() {
        List<TOrder> emptyState = tOrderMapper.findAll();
        assertEquals(0, emptyState.size());
        assertThrows(UncategorizedSQLException.class, () -> {
            tOrderMapper.addByNameAndUserId("Bright", 114514L);
            tOrderMapper.addByNameAndUserId("Jordan", 114515L);
        });
    }

    @Test
    void whenRequestToDeleteByIdTest() {
        tOrderMapper.addAll(101L, "Bright", 114514L);
        tOrderMapper.addAll(102L, "Jordan", 114515L);
        tOrderMapper.addAll(103L, "Lemon", 114516L);
        tOrderMapper.addAll(104L, "Jack", 114517L);
        tOrderMapper.addAll(105L, "Michael", 114518L);
        tOrderMapper.addAll(106L, "Tony", 114519L);
        int numberOfAffectedRows = tOrderMapper.deleteById(114514L);
        assertEquals(1, numberOfAffectedRows);
        assertEquals(5, tOrderMapper.findAll().size());
    }

    @Test
    void whenRequestToDeleteAll() {
        tOrderMapper.addAll(101L, "Bright", 114514L);
        tOrderMapper.addAll(102L, "Jordan", 114515L);
        tOrderMapper.addAll(103L, "Lemon", 114516L);
        tOrderMapper.addAll(104L, "Jack", 114517L);
        tOrderMapper.addAll(105L, "Michael", 114518L);
        tOrderMapper.addAll(106L, "Tony", 114519L);
        int numberOfAffectedRows = tOrderMapper.deleteAll();
        assertEquals(6, numberOfAffectedRows);
        assertEquals(0, tOrderMapper.findAll().size());
    }
}
