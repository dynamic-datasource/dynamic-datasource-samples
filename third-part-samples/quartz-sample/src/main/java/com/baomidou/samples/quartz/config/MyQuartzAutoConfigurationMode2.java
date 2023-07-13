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
package com.baomidou.samples.quartz.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

//测试的时候请打开这个，并关闭其他Mode的配置
//@Configuration
public class MyQuartzAutoConfigurationMode2 {

    @Autowired
    private DataSourceProperties dataSourceProperties;
    @Autowired
    private DynamicDataSourceProperties properties;

    @Primary
    @Bean
    public DataSource dataSource() {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrict(properties.getStrict());
        dataSource.setStrategy(properties.getStrategy());
        dataSource.setP6spy(properties.getP6spy());
        dataSource.setSeata(properties.getSeata());
        return dataSource;
    }

    @QuartzDataSource
    @Bean
    public DataSource quartzDataSource() {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    //如果需要使用动态数据源里的某个数据源则打开以下配置，关闭上面配置。
//    @QuartzDataSource
//    @Bean
//    public DataSource quartzDataSource(DataSource dataSource) {
//        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
//        return ds.getDataSource("quartz");
//    }
}