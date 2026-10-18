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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 只使用 Jackson 的树模型：不做 POJO 绑定，native image 中也就不需要为业务类型登记反射。 */
final class Json {

    static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {}

    static JsonNode parse(String json) {
        return json == null || json.isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(json);
    }

    /** 字段缺失或为 JSON null 时返回 null。 */
    static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    static String required(JsonNode node, String field) {
        var value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new FacadeException(ErrorCode.BAD_REQUEST, "'%s' is required".formatted(field));
        }
        return value;
    }
}
