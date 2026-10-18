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

/** 带有稳定 {@link ErrorCode} 的失败，由门面统一转成 {@code {"ok":false,"error":{...}}}。 */
final class FacadeException extends RuntimeException {

    private final ErrorCode code;

    FacadeException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    ErrorCode code() {
        return code;
    }
}
