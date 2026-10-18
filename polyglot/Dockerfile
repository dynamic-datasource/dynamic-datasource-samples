# syntax=docker/dockerfile:1
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

#
# 构建上下文是仓库根目录，例如 `docker build -f polyglot/Dockerfile --target rust-test .`。
#
# - libdynamicds：用 GraalVM 构建共享库，三个示例共用这一阶段；示例镜像用 DYNAMIC_DATASOURCE_LIB_DIR（Rust）/
#   DYNAMIC_DATASOURCE_LIB（Python、TypeScript）指向它，各示例的构建因此不再自己构建共享库；
# - rust / python / typescript：运行镜像；
# - rust-test / python-test / typescript-test：构建时做格式检查、lint 与编译；测试用 Testcontainers 启动 PostgreSQL，
#   需要 Docker，所以在容器运行时执行（见 compose.yaml 的 test profile）。

FROM ghcr.io/graalvm/native-image-community:25i3-ol9 AS libdynamicds
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/
COPY polyglot/pom.xml polyglot/
COPY polyglot/dynamic-datasource-ffi polyglot/dynamic-datasource-ffi
# 只构建这一个模块：-f 指向它的 pom，父 pom 按 relativePath 解析，其余模块不参与
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -e -Pnative -DskipTests -f polyglot/dynamic-datasource-ffi/pom.xml package \
    && mkdir /out \
    && cp polyglot/dynamic-datasource-ffi/target/libdynamicds.so polyglot/dynamic-datasource-ffi/target/*.h /out/

# ---------------------------------------------------------------------------------------------------- Rust

FROM rust:1.99-slim-trixie AS rust-build
# 官方镜像是 minimal profile，不带 rustfmt 与 clippy；下载偶尔失败，重试几次
RUN for i in 1 2 3; do rustup component add rustfmt clippy && exit 0; sleep 5; done; exit 1
COPY --from=libdynamicds /out/libdynamicds.so /usr/local/lib/
ENV DYNAMIC_DATASOURCE_LIB_DIR=/usr/local/lib
WORKDIR /app/polyglot/rust-axum-sample
# 先只拉依赖：Cargo.toml、Cargo.lock 不变时这一层直接命中缓存，之后的构建与容器里的测试都不再联网
COPY polyglot/rust-axum-sample/Cargo.toml polyglot/rust-axum-sample/Cargo.lock ./
RUN mkdir src && touch src/lib.rs && cargo fetch --locked && rm -r src
ENV CARGO_NET_OFFLINE=true
COPY polyglot/rust-axum-sample ./
RUN cargo build --release --locked && cp target/release/rust-axum-sample /usr/local/bin/

FROM rust-build AS rust-test
ENV DD_LOG_LEVEL=warn
RUN cargo fmt --check \
    && cargo clippy --all-targets --locked -- -D warnings \
    && cargo test --locked --no-run
CMD ["cargo", "test", "--locked"]

FROM debian:trixie-slim AS rust
COPY --from=libdynamicds /out/libdynamicds.so /usr/local/lib/
# 运行镜像里只有二进制：示例自带的 config/ 与 db/ 另外复制，再用 SAMPLE_DIR 指过去
COPY polyglot/rust-axum-sample/config /app/config
COPY polyglot/rust-axum-sample/db /app/db
COPY --from=rust-build /usr/local/bin/rust-axum-sample /usr/local/bin/
ENV SAMPLE_DIR=/app PORT=8081
EXPOSE 8081
USER nobody
CMD ["rust-axum-sample"]

# ---------------------------------------------------------------------------------------------------- Python

FROM python:3.14-slim-trixie AS python-base
COPY --from=ghcr.io/astral-sh/uv:0.12 /uv /usr/local/bin/uv
COPY --from=libdynamicds /out/libdynamicds.so /usr/local/lib/
ENV DYNAMIC_DATASOURCE_LIB=/usr/local/lib/libdynamicds.so \
    PYTHONUNBUFFERED=1 UV_LINK_MODE=copy UV_PYTHON_DOWNLOADS=never
WORKDIR /app/polyglot/python-fastapi-sample
COPY polyglot/python-fastapi-sample/pyproject.toml polyglot/python-fastapi-sample/uv.lock polyglot/python-fastapi-sample/.python-version ./

FROM python-base AS python-test
# 只装依赖：测试经由 pythonpath 直接导入 app，本项目不必安装
RUN --mount=type=cache,target=/root/.cache/uv uv sync --locked --no-install-project
COPY polyglot/python-fastapi-sample ./
# 环境已经就绪，之后的 uv run 不再同步，容器里跑测试时也不需要联网
ENV DD_LOG_LEVEL=warn UV_NO_SYNC=1
RUN uv run ruff format --check . && uv run ruff check .
CMD ["uv", "run", "pytest", "-q"]

FROM python-base AS python
RUN --mount=type=cache,target=/root/.cache/uv uv sync --locked --no-dev --no-install-project
COPY polyglot/python-fastapi-sample ./
ENV PATH=/app/polyglot/python-fastapi-sample/.venv/bin:$PATH PORT=8082
EXPOSE 8082
USER nobody
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port \"$PORT\""]

# ---------------------------------------------------------------------------------------------------- TypeScript

FROM node:26-trixie-slim AS typescript-base
COPY --from=libdynamicds /out/libdynamicds.so /usr/local/lib/
ENV DYNAMIC_DATASOURCE_LIB=/usr/local/lib/libdynamicds.so
WORKDIR /app/polyglot/typescript-hono-sample
COPY polyglot/typescript-hono-sample/package.json polyglot/typescript-hono-sample/package-lock.json ./

FROM typescript-base AS typescript-test
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY polyglot/typescript-hono-sample ./
ENV DD_LOG_LEVEL=warn
RUN npm run format:check && npm run typecheck
CMD ["npm", "test"]

FROM typescript-base AS typescript
RUN --mount=type=cache,target=/root/.npm npm ci --omit=dev --no-audit --no-fund
COPY polyglot/typescript-hono-sample ./
ENV PORT=8083
EXPOSE 8083
USER node
CMD ["node", "--disable-warning=ExperimentalWarning", "src/server.ts"]
