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
package com.baomidou.samples.load;

import com.baomidou.dynamic.datasource.common.DataSourceProperty;
import com.baomidou.dynamic.datasource.provider.AbstractJdbcDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class LoadDsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadDsApplication.class, args);
    }

    @Bean
    public DynamicDataSourceProvider dynamicDataSourceProvider() {
        return new AbstractJdbcDataSourceProvider("org.h2.Driver", "jdbc:h2:mem:test;MODE=MySQL", "sa", "") {
            @Override
            protected Map<String, DataSourceProperty> executeStmt(Statement statement)
                    throws SQLException {
                //*************** 实际运行应直接从库中查出，演示所以需要先插入数据***************
                statement.execute("CREATE TABLE IF NOT EXISTS `DB`\n"
                        + "(\n"
                        + "    `name`   VARCHAR(30) NULL DEFAULT NULL,\n"
                        + "    `username`   VARCHAR(30) NULL DEFAULT NULL,\n"
                        + "    `password`   VARCHAR(30) NULL DEFAULT NULL,\n"
                        + "    `url`   VARCHAR(30) NULL DEFAULT NULL,\n"
                        + "    `driver`   VARCHAR(30) NULL DEFAULT NULL\n"
                        + ")");
                statement.executeUpdate(
                        "insert into DB values ('db1','sa','','jdbc:h2:mem:test2','org.h2.Driver')");
                statement.executeUpdate(
                        "insert into DB values ('db2','sa','','jdbc:h2:mem:test3','org.h2.Driver')");
                statement.executeUpdate(
                        "insert into DB values ('db3','sa','','jdbc:h2:mem:test4','org.h2.Driver')");
                Map<String, DataSourceProperty> map = new HashMap<>();
                //*************** ↑↑↑↑↑↑↑ END  ***************

                ResultSet rs = statement.executeQuery("select * from DB");
                while (rs.next()) {
                    String name = rs.getString("name");
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    String url = rs.getString("url");
                    String driver = rs.getString("driver");
                    DataSourceProperty property = new DataSourceProperty();
                    property.setUsername(username);
                    property.setPassword(password);
                    property.setUrl(url);
                    property.setDriverClassName(driver);
                    map.put(name, property);
                }
                return map;
            }
        };
    }
}