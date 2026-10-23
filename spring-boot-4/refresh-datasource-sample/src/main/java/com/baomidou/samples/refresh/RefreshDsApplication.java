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
package com.baomidou.samples.refresh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 配置中心刷新数据源示例。
 *
 * <p>本示例刻意不连真实的 Nacos：刷新这件事与配置中心无关，换成任何配置中心都只是 「把新配置交给 DynamicDataSourceRefresher」这一步。
 */
@Slf4j
@SpringBootApplication
public class RefreshDsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RefreshDsApplication.class, args);
        log.info(
                "try: curl -X POST localhost:8080/datasources/refresh -H 'Content-Type: application/json' "
                        + "-d '[{\"name\":\"slave_2\",\"url\":\"jdbc:h2:mem:slave2;DB_CLOSE_DELAY=-1\","
                        + "\"username\":\"sa\",\"password\":\"\",\"driverClassName\":\"org.h2.Driver\"}]'");
    }
}
