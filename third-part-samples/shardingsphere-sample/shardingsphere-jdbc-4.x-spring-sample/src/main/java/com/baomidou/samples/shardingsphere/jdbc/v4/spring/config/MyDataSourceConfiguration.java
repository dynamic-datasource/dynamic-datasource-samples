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
package com.baomidou.samples.shardingsphere.jdbc.v4.spring.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.provider.AbstractDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyDataSourceConfiguration {

    private final DynamicDataSourceProperties properties;

    private final DefaultDataSourceCreator dataSourceCreator;

    //    private final DataSource encryptDataSource;

    private final DataSource masterSlaveDataSource;

    //    private final DataSource shadowDataSource;

    private final DataSource shardingDataSource;

    /**
     * 1. 在 `org.apache.shardingsphere:sharding-jdbc-spring-boot-starter：4.1.1`中， 针对数据源的处理远比 ShardingSphere 5.x 要落后。
     * 根据 SpringBootConfiguration，在  ShardingSphere 4.1.1 的 Spring Boot Starter 中，根据是否配置相关规则注册了 0-4 个 DataSource，
     * 分别是 EncryptDataSource，MasterSlaveDataSource， ShadowDataSource，ShardingDataSource。分别处理数据加密，读写分离，影子库，数据分片。
     * 2. 多个数据源的规则并不允许混合使用，只能 `@DS` 到某个子功能的数据源。混合规则在 ShardingSphere 5.x 被引入，参考
     * <a href="https://shardingsphere.apache.org/document/5.4.0/en/user-manual/shardingsphere-jdbc/yaml-config/rules/mix/">MIXED RULES</a> 。
     * 3. example: 把 `shardingDataSource` 加入多数据源，到时候使用的时候就可以 `@DS("shardingDataSourceInShardingSphere")`
     *
     * @see org.apache.shardingsphere.shardingjdbc.spring.boot.SpringBootConfiguration
     * @see javax.sql.DataSource
     * @see org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.EncryptDataSource
     * @see org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.MasterSlaveDataSource
     * @see org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShadowDataSource
     * @see org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShardingDataSource
     */
    public MyDataSourceConfiguration(DynamicDataSourceProperties properties, DefaultDataSourceCreator dataSourceCreator,
//                                     @Lazy @Qualifier("encryptDataSource") DataSource encryptDataSource,
                                     @Lazy
                                     @Qualifier("masterSlaveDataSource") DataSource masterSlaveDataSource,
//                                     @Lazy @Qualifier("shadowDataSource") DataSource shadowDataSource,
                                     @Lazy
                                     @Qualifier("shardingDataSource") DataSource shardingDataSource
    ) {
        this.properties = properties;
        this.dataSourceCreator = dataSourceCreator;
//        this.encryptDataSource = encryptDataSource;
        this.masterSlaveDataSource = masterSlaveDataSource;
//        this.shadowDataSource = shadowDataSource;
        this.shardingDataSource = shardingDataSource;
    }


    @Bean
    public DynamicDataSourceProvider dynamicDataSourceProvider() {
        return new AbstractDataSourceProvider(dataSourceCreator) {
            @Override
            public Map<String, DataSource> loadDataSources() {
                Map<String, DataSource> dataSourceMap = new HashMap<>();
//                dataSourceMap.put("encryptDataSourceInShardingSphere", encryptDataSource);
                dataSourceMap.put("masterSlaveDataSourceInShardingSphere", masterSlaveDataSource);
//                dataSourceMap.put("shadowDataSourceInShardingSphere", shadowDataSource);
                dataSourceMap.put("shardingDataSourceInShardingSphere", shardingDataSource);
                return dataSourceMap;
            }
        };
    }

    /**
     * 将动态数据源设置为首选的
     * 当 Spring 存在多个数据源时, 自动注入的是首选的对象
     * 设置为主要的数据源之后，就可以支持shardingJdbc原生的配置方式了
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
