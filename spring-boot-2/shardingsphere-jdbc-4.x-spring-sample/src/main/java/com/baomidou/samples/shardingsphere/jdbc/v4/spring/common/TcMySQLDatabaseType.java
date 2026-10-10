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
package com.baomidou.samples.shardingsphere.jdbc.v4.spring.common;

import java.util.Collection;
import java.util.Collections;
import org.apache.shardingsphere.spi.database.metadata.DataSourceMetaData;
import org.apache.shardingsphere.spi.database.type.BranchDatabaseType;
import org.apache.shardingsphere.spi.database.type.DatabaseType;
import org.apache.shardingsphere.underlying.common.database.type.dialect.MySQLDatabaseType;

public final class TcMySQLDatabaseType implements BranchDatabaseType {

    private final MySQLDatabaseType trunkDatabaseType = new MySQLDatabaseType();

    @Override
    public String getName() {
        return "TC-MySQL";
    }

    @Override
    public Collection<String> getJdbcUrlPrefixAlias() {
        return Collections.singleton("jdbc:tc:mysql:");
    }

    @Override
    public DataSourceMetaData getDataSourceMetaData(final String url, final String username) {
        return trunkDatabaseType.getDataSourceMetaData(normalizeUrl(url), username);
    }

    @Override
    public DatabaseType getTrunkDatabaseType() {
        return trunkDatabaseType;
    }

    private static String normalizeUrl(final String url) {
        if (url.startsWith("jdbc:tc:mysql:")) {
            int authorityIndex = url.indexOf("://");
            if (-1 != authorityIndex) {
                String afterAuthority = url.substring(authorityIndex);
                int queryIndex = afterAuthority.indexOf('?');
                if (-1 != queryIndex) {
                    afterAuthority = afterAuthority.substring(0, queryIndex);
                }
                return "jdbc:mysql:" + afterAuthority;
            }
        }
        return url;
    }
}
