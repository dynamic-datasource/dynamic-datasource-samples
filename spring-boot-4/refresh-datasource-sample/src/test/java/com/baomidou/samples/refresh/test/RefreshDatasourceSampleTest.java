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
package com.baomidou.samples.refresh.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceRefresher;
import com.baomidou.samples.refresh.controller.RefreshController;
import com.baomidou.samples.refresh.dto.DataSourceDTO;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RefreshDatasourceSampleTest {

    @Autowired private RefreshController controller;

    @Autowired private DataSource dataSource;

    @Test
    void shouldAddAndRemoveDataSourceByRefresh() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        assertThat(ds.getDataSources())
                .containsKeys("master", "slave_1")
                .doesNotContainKey("slave_2");

        List<DataSourceDTO> withNewOne = new ArrayList<>(initialConfig());
        withNewOne.add(h2("slave_2", "jdbc:h2:mem:slave2;DB_CLOSE_DELAY=-1"));

        DynamicDataSourceRefresher.Result added = controller.refresh(withNewOne);
        assertThat(added.getAdded()).containsExactly("slave_2");
        assertThat(ds.getDataSources()).containsKey("slave_2");

        DynamicDataSourceRefresher.Result removed = controller.refresh(initialConfig());
        assertThat(removed.getRemoved()).containsExactly("slave_2");
        assertThat(ds.getDataSources()).doesNotContainKey("slave_2");
    }

    @Test
    void shouldSkipWhenConfigUnchanged() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        DataSource slaveBefore = ds.getDataSources().get("slave_1");

        DynamicDataSourceRefresher.Result result = controller.refresh(initialConfig());

        assertThat(result.isChanged()).isFalse();
        assertThat(result.getSkipped()).contains("master", "slave_1");
        assertThat(ds.getDataSources().get("slave_1")).isSameAs(slaveBefore);
    }

    private static List<DataSourceDTO> initialConfig() {
        List<DataSourceDTO> config = new ArrayList<>();
        config.add(h2("master", "jdbc:h2:mem:master;DB_CLOSE_DELAY=-1"));
        config.add(h2("slave_1", "jdbc:h2:mem:slave1;DB_CLOSE_DELAY=-1"));
        return config;
    }

    private static DataSourceDTO h2(String name, String url) {
        DataSourceDTO dto = new DataSourceDTO();
        dto.setName(name);
        dto.setUrl(url);
        dto.setUsername("sa");
        dto.setPassword("");
        dto.setDriverClassName("org.h2.Driver");
        return dto;
    }
}
