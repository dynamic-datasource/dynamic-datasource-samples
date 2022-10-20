package com.baomidou.samples.shardingsphere.jdbc.v5.spring;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity.TOrder;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.mapper.TOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;

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
        int firstNumberOfAffectedRows = tOrderMapper.addAll(101L, "Bright", 114514L);
        int secondNumberOfAffectedRows = tOrderMapper.addAll(102L, "Jordan", 114515L);
        assertEquals(firstNumberOfAffectedRows + secondNumberOfAffectedRows, 2);
        assertEquals(tOrderMapper.findAll().size(), 2);
    }

    @Test
    void whenRequestToAddByNameAndUserIdByMultipleDataNodes() {
        List<TOrder> emptyState = tOrderMapper.findAll();
        assertEquals(emptyState.size(), 0);
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
        assertEquals(numberOfAffectedRows, 1);
        assertEquals(tOrderMapper.findAll().size(), 5);
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
        assertEquals(numberOfAffectedRows, 6);
        assertEquals(tOrderMapper.findAll().size(), 0);
    }
}
