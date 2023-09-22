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
package com.baomidou.samples.shardingsphere.jdbc.v5.spring.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.provider.AbstractDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyDataSourceConfiguration {
    private final DynamicDataSourceProperties properties;
    private final DefaultDataSourceCreator dataSourceCreator;
    private final DataSource shardingSphereDataSource;

    /**
     * 1. 建议 spring-boot 2.5.0 以下版本或者发现不加 `@Lazy`, `DataSource shardingSphereDataSource` 是 null 的情况都打开 `@Lazy`
     * 2. Compared with using SpringBoot Starter, if you encounter such problems,
     * you should directly use ShardingSphere's JDBC Driver to configure it as a JDBC data source, that is,
     * use `org.apache.shardingsphere:shardingsphere-jdbc-core:5.2.1` instead of `org.apache.shardingsphere:shardingsphere-jdbc-core-spring-boot-starter:5.2.1`.
     * For more information see <a href="https://shardingsphere.apache.org/document/5.2.1/en/user-manual/shardingsphere-jdbc/yaml-config/jdbc_driver/">JDBC Driver</a>
     *
     * @see org.springframework.context.annotation.Lazy
     * @see org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource
     */
    public MyDataSourceConfiguration(DynamicDataSourceProperties properties,
                                     DefaultDataSourceCreator dataSourceCreator,
                                     @Qualifier("shardingSphereDataSource") DataSource shardingSphereDataSource) {
        this.properties = properties;
        this.dataSourceCreator = dataSourceCreator;
        this.shardingSphereDataSource = shardingSphereDataSource;
    }

    @Bean
    public DynamicDataSourceProvider dynamicDataSourceProvider() {
        return new AbstractDataSourceProvider(dataSourceCreator) {
            @Override
            public Map<String, DataSource> loadDataSources() {
                Map<String, DataSource> dataSourceMap = new HashMap<>();
                // 把 shardingSphereDataSource 加入多数据源，到时候使用的时候就可以 `@DS("shardingSphere")`
                dataSourceMap.put("shardingSphere", shardingSphereDataSource);
                return dataSourceMap;
            }
        };
    }

    /**
     * 将 dynamic-datasource 设置为首选的
     * 当 Spring 存在多个数据源时, 自动注入的是首选的对象
     * 设置为主要的数据源之后，就可以支持 shardingSphere 原生的配置方式了
     */
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
}
