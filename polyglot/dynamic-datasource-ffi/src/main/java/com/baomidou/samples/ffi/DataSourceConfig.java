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

import static java.util.stream.Collectors.toMap;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

/**
 * {@code dd_open} 的配置，字段与 {@code spring.datasource.dynamic.*} 同名（camelCase）。
 *
 * <p>字符串值里的 {@code ${NAME}} / {@code ${NAME:default}} 占位符先取顶层 {@code variables}（由宿主语言传入， 例如 SQL
 * 脚本目录 {@code DB_DIR}），再取环境变量，最后取默认值。
 */
record DataSourceConfig(
        String primary,
        boolean strict,
        boolean randomStrategy,
        Boolean lazy,
        Duration txIdleTimeout,
        Map<String, DataSourceProperty> datasources) {

    private static final PropertyPlaceholderHelper PLACEHOLDERS =
            new PropertyPlaceholderHelper("${", "}", ":", null, false);

    static DataSourceConfig parse(JsonNode root) {
        if (!(root instanceof ObjectNode object)) {
            throw new FacadeException(ErrorCode.BAD_REQUEST, "the configuration must be an object");
        }
        var variables =
                object.path("variables")
                        .propertyStream()
                        .collect(toMap(Map.Entry::getKey, e -> e.getValue().asString()));
        PlaceholderResolver resolver = name -> variables.getOrDefault(name, System.getenv(name));
        // variables 是占位符的取值来源，自身不参与解析
        var cfg = resolve(object.deepCopy().without("variables"), resolver);
        var datasources = new LinkedHashMap<String, DataSourceProperty>();
        cfg.path("datasource")
                .properties()
                .forEach(e -> datasources.put(e.getKey(), toProperty(e.getKey(), e.getValue())));
        if (datasources.isEmpty()) {
            throw new FacadeException(ErrorCode.BAD_REQUEST, "'datasource' must not be empty");
        }
        var randomStrategy =
                switch (cfg.path("strategy").asString("loadbalance")) {
                    case "loadbalance" -> false;
                    case "random" -> true;
                    default ->
                            throw new FacadeException(
                                    ErrorCode.BAD_REQUEST,
                                    "'strategy' must be 'loadbalance' or 'random'");
                };
        return new DataSourceConfig(
                cfg.path("primary").asString("master"),
                cfg.path("strict").asBoolean(false),
                randomStrategy,
                cfg.optional("lazy").map(JsonNode::asBoolean).orElse(null),
                Duration.ofSeconds(cfg.path("txIdleTimeoutSeconds").asLong(30)),
                datasources);
    }

    /** 与 {@code spring.datasource.dynamic.datasource.<name>.*} 对应，hikari 只映射常用的三项。 */
    static DataSourceProperty toProperty(String name, JsonNode node) {
        var property = new DataSourceProperty();
        property.setPoolName(name);
        property.setUrl(Json.required(node, "url"));
        property.setUsername(Json.text(node, "username"));
        property.setPassword(Json.text(node, "password"));
        property.setDriverClassName(Json.text(node, "driverClassName"));
        node.optional("lazy").map(JsonNode::asBoolean).ifPresent(property::setLazy);
        var init = node.path("init");
        var initProperty = property.getInit();
        initProperty.setSchema(Json.text(init, "schema"));
        initProperty.setData(Json.text(init, "data"));
        init.optional("continueOnError")
                .map(JsonNode::asBoolean)
                .ifPresent(initProperty::setContinueOnError);
        var hikari = node.path("hikari");
        var hikariConfig = property.getHikari();
        hikari.optional("maxPoolSize").map(JsonNode::asInt).ifPresent(hikariConfig::setMaxPoolSize);
        hikari.optional("minIdle").map(JsonNode::asInt).ifPresent(hikariConfig::setMinIdle);
        hikari.optional("connectionTimeout")
                .map(JsonNode::asLong)
                .ifPresent(hikariConfig::setConnectionTimeout);
        return property;
    }

    private static JsonNode resolve(JsonNode node, PlaceholderResolver resolver) {
        return switch (node) {
            case StringNode string -> {
                try {
                    yield StringNode.valueOf(
                            PLACEHOLDERS.replacePlaceholders(string.asString(), resolver));
                } catch (IllegalArgumentException e) {
                    // 既没有对应的 variables / 环境变量，也没有写默认值
                    throw new FacadeException(ErrorCode.BAD_REQUEST, e.getMessage());
                }
            }
            case ObjectNode object -> {
                var copy = Json.MAPPER.createObjectNode();
                object.properties()
                        .forEach(e -> copy.set(e.getKey(), resolve(e.getValue(), resolver)));
                yield copy;
            }
            case ArrayNode array -> {
                var copy = Json.MAPPER.createArrayNode();
                array.forEach(item -> copy.add(resolve(item, resolver)));
                yield copy;
            }
            default -> node;
        };
    }
}
