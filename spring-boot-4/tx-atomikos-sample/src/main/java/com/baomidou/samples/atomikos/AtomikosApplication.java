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
package com.baomidou.samples.atomikos;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Atomikos XA 分布式事务样例的启动类。
 *
 * <p>{@code @MapperScan} 显式声明 {@code sqlSessionFactoryRef}，指向本模块 {@link
 * com.baomidou.samples.atomikos.config.MyBatisConfig} 中装配了 {@code AtomikosTransactionFactory} 的
 * SqlSessionFactory，同时避免 AOT 下 AUTOWIRE_BY_TYPE 装配丢失（JVM 行为不变）。
 */
@Slf4j
@SpringBootApplication
@MapperScan(
        value = "com.baomidou.samples.atomikos.mapper",
        sqlSessionFactoryRef = "sqlSessionFactory")
public class AtomikosApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtomikosApplication.class, args);
        log.info("open http://localhost:8080/swagger-ui.html");
    }
}
