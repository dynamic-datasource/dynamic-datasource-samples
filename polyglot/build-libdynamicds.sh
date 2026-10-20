#!/bin/sh
#
# Copyright © ${project.inceptionYear} organization baomidou
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# 三个示例的构建（build.rs、hatch_build.py、npm 的 pretest / prestart）都调用本脚本：libdynamicds.so 不存在，
# 或比 Java 源码（src/main）与 pom 旧时，用 GraalVM 重新构建它，否则什么也不做。
# 找不到 GraalVM 的 native-image 时只打印警告并正常退出，不需要共享库的 cargo clippy、ruff 等照常运行。
set -eu

polyglot=$(cd "$(dirname "$0")" && pwd)
module=$polyglot/dynamic-datasource-ffi
library=$module/target/libdynamicds.so

if [ -f "$library" ] && [ -z "$(find "$module/src/main" "$module/pom.xml" "$polyglot/pom.xml" "$polyglot/../pom.xml" \
    -newer "$library" | head -n 1)" ]; then
    exit 0
fi

# 与 native-maven-plugin 一样，依次在 GRAALVM_HOME、JAVA_HOME 与 PATH 里找 native-image
if [ ! -x "${GRAALVM_HOME:-}/bin/native-image" ] && [ ! -x "${JAVA_HOME:-}/bin/native-image" ] \
    && ! command -v native-image >/dev/null 2>&1; then
    echo "warning: 找不到 GraalVM 的 native-image，跳过 libdynamicds 的构建。请安装 GraalVM 25，让 JAVA_HOME" \
        "或 GRAALVM_HOME 指向它；或者用 DYNAMIC_DATASOURCE_LIB_DIR（Rust）/" \
        "DYNAMIC_DATASOURCE_LIB（Python、TypeScript）指定现成的 libdynamicds.so" >&2
    exit 0
fi

echo "libdynamicds.so 不存在或已过期，用 GraalVM 构建，需要几分钟" >&2
# 与 Dockerfile 一样用 -f 只构建这一个模块，不依赖根 pom 按 JDK 版本激活的 polyglot profile
cd "$polyglot/.."
exec ./mvnw -B -e -Pnative -DskipTests -f polyglot/dynamic-datasource-ffi/pom.xml package
