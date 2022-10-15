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
package com.baomidou.samples.shardingsphere.jdbc.v5.spring.util;

import org.apache.shardingsphere.sharding.cosid.algorithm.keygen.CosIdSnowflakeKeyGenerateAlgorithm;

import java.util.Properties;

/**
 * 1. In actual scenarios, you don't really need to create an util class to use ShardingSphere's key generation algorithm,
 * but you should actively use ShardingSphere's internal integration to shield this operation.
 * 2. But because some of my friends want to see the process of generating algorithm IDs, I specially wrote an util class.
 */
public class SnowFlakeUtil {
    private static final CosIdSnowflakeKeyGenerateAlgorithm cosIdSnowflakeKeyGenerateAlgorithm;

    static {
        cosIdSnowflakeKeyGenerateAlgorithm = new CosIdSnowflakeKeyGenerateAlgorithm();
        Properties properties = new Properties();
        cosIdSnowflakeKeyGenerateAlgorithm.init(properties);
    }

    public static long createId() {
        return (Long) cosIdSnowflakeKeyGenerateAlgorithm.generateKey();
    }
}