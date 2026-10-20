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
package com.baomidou.samples.atomikos.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;

/**
 * Atomikos XA 场景下 MyBatis 的关键装配。
 *
 * <p>多数据源 + XA 时，MyBatis 必须使用 dynamic-datasource 提供的 {@code
 * com.baomidou.dynamic.datasource.tx.AtomikosTransactionFactory}：它在每次 MyBatis 事务开始时先按当前 {@code @DS}
 * 线程变量确定真实数据源，再交给 SpringManagedTransaction， 而不是把动态路由数据源本身交给 MyBatis。
 *
 * <p>声明名为 {@code sqlSessionFactory} 的 Bean 后，MybatisAutoConfiguration 的自动装配会退让， 由本配置全权负责。
 */
@org.springframework.context.annotation.Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTransactionFactory(
                new com.baomidou.dynamic.datasource.tx.AtomikosTransactionFactory());
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }
}
