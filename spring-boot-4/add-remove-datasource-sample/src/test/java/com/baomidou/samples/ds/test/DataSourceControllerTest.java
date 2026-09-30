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
package com.baomidou.samples.ds.test;

import com.baomidou.samples.ds.controller.DataSourceController;
import com.baomidou.samples.ds.dto.DataSourceDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSourceControllerTest {

    @Autowired
    private DataSourceController dataSourceController;

    @Test
    void addAndRemoveDatasource() {
        assertThat(dataSourceController.now()).doesNotContain("test_runtime");

        DataSourceDTO dto = new DataSourceDTO();
        dto.setPoolName("test_runtime");
        dto.setDriverClassName("org.h2.Driver");
        dto.setUrl("jdbc:h2:mem:test_runtime");
        dto.setUsername("sa");
        dto.setPassword("");
        assertThat(dataSourceController.add(dto)).contains("test_runtime");

        assertThat(dataSourceController.remove("test_runtime")).isNotNull();
        assertThat(dataSourceController.now()).doesNotContain("test_runtime");
    }
}
