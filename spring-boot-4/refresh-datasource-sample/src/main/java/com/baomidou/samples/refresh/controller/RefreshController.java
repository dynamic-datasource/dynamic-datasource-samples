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
package com.baomidou.samples.refresh.controller;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceRefresher;
import com.baomidou.samples.refresh.dto.DataSourceDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据源刷新入口。
 *
 * <p>{@code POST /datasources/refresh} 等价于配置中心推送了一份新配置：把整份数据源清单交给 {@link
 * DynamicDataSourceRefresher#refresh}，由它做 diff 后只重建变化的连接池。 接入 Spring Cloud 时这一步由框架自带的监听器完成，无需本接口。
 */
@RestController
@RequestMapping("/datasources")
public class RefreshController {

    private final DataSource dataSource;
    private final DynamicDataSourceRefresher refresher;

    public RefreshController(DataSource dataSource, DynamicDataSourceRefresher refresher) {
        this.dataSource = dataSource;
        this.refresher = refresher;
    }

    /** 当前所有数据源 */
    @GetMapping
    public Set<String> now() {
        return routing().getDataSources().keySet();
    }

    /** 用一份新配置刷新数据源，返回本次新增/更新/移除/跳过的明细 */
    @PostMapping("/refresh")
    public DynamicDataSourceRefresher.Result refresh(@RequestBody List<DataSourceDTO> config) {
        Map<String, DataSourceProperty> next = new LinkedHashMap<>();
        for (DataSourceDTO item : config) {
            DataSourceProperty property = new DataSourceProperty();
            property.setUrl(item.getUrl());
            property.setUsername(item.getUsername());
            property.setPassword(item.getPassword());
            property.setDriverClassName(item.getDriverClassName());
            next.put(item.getName(), property);
        }
        return refresher.refresh(next);
    }

    private DynamicRoutingDataSource routing() {
        return (DynamicRoutingDataSource) dataSource;
    }
}
