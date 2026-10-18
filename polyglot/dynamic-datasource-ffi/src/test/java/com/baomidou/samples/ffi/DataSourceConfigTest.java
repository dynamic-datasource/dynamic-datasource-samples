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
package com.baomidou.samples.ffi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DataSourceConfigTest {

    @Test
    void resolvesPlaceholdersFromVariablesThenEnvironmentThenDefaults() {
        var config =
                DataSourceConfig.parse(
                        Json.parse(
                                """
                                {
                                  "variables": {"HOST": "db.internal", "PATH": "from-variables"},
                                  "datasource": {
                                    "master": {
                                      "url": "jdbc:postgresql://${HOST}:${DD_TEST_UNDEFINED_PORT:5432}/ds",
                                      "username": "${PATH}",
                                      "password": "${HOME}"
                                    }
                                  }
                                }
                                """));
        var master = config.datasources().get("master");
        assertThat(master.getUrl()).isEqualTo("jdbc:postgresql://db.internal:5432/ds");
        // variables 优先于同名的环境变量
        assertThat(master.getUsername()).isEqualTo("from-variables");
        assertThat(master.getPassword()).isEqualTo(System.getenv("HOME"));
        assertThat(config.primary()).isEqualTo("master");
        assertThat(config.strict()).isFalse();
        assertThat(config.randomStrategy()).isFalse();
        assertThat(config.txIdleTimeout()).hasSeconds(30);
    }

    @Test
    void rejectsUnresolvablePlaceholder() {
        assertBadRequest(
                """
                {"datasource": {"master": {"url": "${DD_TEST_UNDEFINED}"}}}
                """);
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertBadRequest(
                """
                {"datasource": {}}
                """);
        assertBadRequest(
                """
                {"strategy": "fastest", "datasource": {"master": {"url": "jdbc:h2:mem:x"}}}
                """);
        assertBadRequest("[]");
    }

    private static void assertBadRequest(String configuration) {
        assertThatThrownBy(() -> DataSourceConfig.parse(Json.parse(configuration)))
                .isInstanceOfSatisfying(
                        FacadeException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
