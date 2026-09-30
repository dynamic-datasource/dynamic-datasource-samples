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
package com.baomidou.samples.consul.controller;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.Set;

@RestController
@RequestMapping("/datasources")
public class DataSourceController {

    @Autowired
    private DataSource dataSource;

    /**
     * 获取当前所有数据源
     */
    @GetMapping
    public Set<String> now() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        return ds.getDataSources().keySet();
    }
}
