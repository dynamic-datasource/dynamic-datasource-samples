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

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;

class H2DynamicDataSourceFacadeTest extends AbstractDynamicDataSourceFacadeTest {

    @BeforeAll
    static void openH2() throws IOException {
        open("h2", Map.of());
    }

    @Override
    String slave3() {
        var json =
                """
                {
                  "name": "slave_3",
                  "url": "jdbc:h2:mem:ds_slave_3;DB_CLOSE_DELAY=-1",
                  "username": "sa",
                  "password": "",
                  "driverClassName": "org.h2.Driver",
                  "init": {"schema": "file:%s"}
                }
                """;
        return json.formatted(RESOURCES.resolve("db/user/schema.sql"));
    }
}
