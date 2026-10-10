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
package com.baomidou.samples.shardingsphere.jdbc.v5.spring.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.shardingsphere.infra.database.metadata.DataSourceMetaData;
import org.apache.shardingsphere.infra.database.type.BranchDatabaseType;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.sql.parser.sql.common.constant.QuoteCharacter;

public final class TcMySQLDatabaseType implements BranchDatabaseType {

    private final MySQLDatabaseType trunkDatabaseType = new MySQLDatabaseType();

    @Override
    public Collection<String> getJdbcUrlPrefixes() {
        return Collections.singleton("jdbc:tc:mysql:");
    }

    @Override
    public DatabaseType getTrunkDatabaseType() {
        return trunkDatabaseType;
    }

    @Override
    public QuoteCharacter getQuoteCharacter() {
        return trunkDatabaseType.getQuoteCharacter();
    }

    @Override
    public DataSourceMetaData getDataSourceMetaData(final String url, final String username) {
        return trunkDatabaseType.getDataSourceMetaData(normalizeUrl(url), username);
    }

    @Override
    public Map<String, Collection<String>> getSystemDatabaseSchemaMap() {
        return trunkDatabaseType.getSystemDatabaseSchemaMap();
    }

    @Override
    public Collection<String> getSystemSchemas() {
        return trunkDatabaseType.getSystemSchemas();
    }

    @Override
    public String getType() {
        return "TC-MySQL";
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
