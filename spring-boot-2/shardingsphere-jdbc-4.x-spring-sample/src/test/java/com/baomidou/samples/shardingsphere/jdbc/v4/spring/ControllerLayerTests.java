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
import com.baomidou.samples.shardingsphere.jdbc.v4.spring.mapper.TOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Disabled("TODO wait for com.baomidou:dynamic-datasource-spring-boot-starter:4.2.0 release")
public class ControllerLayerTests {
    MockMvc mockMvc;

    @Autowired
    TOrderMapper tOrderMapper;

    @BeforeEach
    void setup(WebApplicationContext webApplicationContext) {
        this.mockMvc = webAppContextSetup(webApplicationContext).defaultResponseCharacterEncoding(StandardCharsets.UTF_8).build();
    }

    @AfterEach
    void after() {
        DynamicDataSourceContextHolder.push("shardingDataSourceInShardingSphere");
        tOrderMapper.deleteAll();
        DynamicDataSourceContextHolder.clear();
    }

    @Test
    void whenGetRequestToFindAll() throws Exception {
        mockMvc.perform(get("/t_order/findAll")
                        .characterEncoding(StandardCharsets.UTF_8))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().encoding(StandardCharsets.UTF_8)
                );
    }

    @Test
    void whenPostRequestToAddAll() throws Exception {
        mockMvc.perform(post("/t_order/addAll")
                        .characterEncoding(StandardCharsets.UTF_8))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().encoding(StandardCharsets.UTF_8)
                );
    }
}
