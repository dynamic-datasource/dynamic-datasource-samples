# Contributing

我们欢迎社区的贡献。围绕此 git 的讨论与协作应通过 https://github.com/baomidou/dynamic-datasource 进行。
在提交 Pull Request 之前, 请在本地通过 [OpenJDK 17, OpenJDK 27] 的 JDK 范围下完成此命令的验证。
我们鼓励通过 `version-fox/vfox` 切换到 `25.3.4.1-graalce` 来验证。

针对 `JetBrains/intellij-community` 等 IDE，`spring-boot-2` 下模块的语言级别应设置为 JDK 8，
`spring-boot-3` 与 `spring-boot-4` 下模块的语言级别应设置为 JDK 17，`polyglot/dynamic-datasource-ffi` 应设置为 JDK 25。
下文讨论不同情况下可能的测试流程，

## Ubuntu 26.04

假设 `git/git`，`version-fox/vfox`，`Docker Engine` 已安装，在 Bash 执行如下命令，

```shell
vfox add java
vfox install java@25.3.4.1-graalce
vfox use --global java@25.3.4.1-graalce
git clone git@github.com:dynamic-datasource/dynamic-datasource-samples.git
cd ./dynamic-datasource-samples/
./mvnw -T1C -e clean test
```

## Windows 11

假设 `git-for-windows/git`，`PowerShell/PowerShell`，`rancher-sandbox/rancher-desktop`，`version-fox/vfox` 已安装，
在 PowerShell 7 执行如下命令，

```shell
rdctl start --application.start-in-background --container-engine.name=moby --kubernetes.enabled=false
vfox add java
vfox install java@25.3.4.1-graalce
vfox use --global java@25.3.4.1-graalce
git clone git@github.com:dynamic-datasource/dynamic-datasource-samples.git
cd ./dynamic-datasource-samples/
./mvnw -T1C -e clean test
```

## Dev Container

本机没有 GraalVM、Rust、uv 或 Node.js 时，可以使用 [`.devcontainer/`](./.devcontainer/devcontainer.json) 描述的开发容器。
它基于 Ubuntu 24.04，装有 GraalVM CE 25（含 `native-image`）、Rust（含 rustfmt 与 clippy）、uv 与 Node.js 26，
另有一个容器内独立的 Docker 引擎，Testcontainers 直接使用它，不需要设置 `DOCKER_HOST`。本机只需要 Docker，再任选一种方式打开，

- VS Code：安装 Dev Containers 扩展后执行 `Dev Containers: Reopen in Container`
- IntelliJ IDEA：打开 `.devcontainer/devcontainer.json`，点击左侧 gutter 中的图标，选择 `Create Dev Container and Mount Sources…`
- GitHub Codespaces：在仓库页面的 `Code` → `Codespaces` 中创建
- 命令行：使用 `@devcontainers/cli`，

  ```shell
  npx @devcontainers/cli up --workspace-folder .
  npx @devcontainers/cli exec --workspace-folder . ./mvnw -T1C -e clean test
  ```

第一次创建容器要下载各个工具链，需要几分钟。进入容器后，上面的 `./mvnw -T1C -e clean test` 与下文 `polyglot/` 示例的命令
都可以直接执行。容器里 uv 的虚拟环境在 `/opt/venvs/` 下，与本机在工作区里创建的 `.venv` 互不影响。
CI 的 `Dev Container CI` 任务会在这个容器里执行 `polyglot/` 的验证命令，修改 `.devcontainer/` 后可以靠它确认容器仍然可用。

宿主机使用 rootless Docker 时，容器里默认的 `vscode` 用户没有工作区的写权限，需要先在启动 IDE 或 devcontainer CLI 的环境里
`export DEVCONTAINER_USER=root`。rootless Docker 下容器里的 root 对应宿主机的当前用户，新建的文件仍然归当前用户所有。

`devcontainer.json` 里不写注释，需要说明的配置记在这里：`extensions` 只列了 Python、Ruff 与 Prettier，
Java、Rust、Node.js 与 Docker 的 VS Code 扩展由对应的 Feature 一并安装。

## 单元测试说明

- 纯 H2 的模块（`mybatisplus3-sample`、`beetlsql-sample`、`jdbc-template-sample`、`name-pattern-sample`、
  `add-remove-datasource-sample`、`load-datasource-from-jdbc-sample`、`druid-sample`、`spel-sample`）可直接 `mvn test`，无需外部服务。
- 涉及外部中间件的模块使用 [Testcontainers](https://java.testcontainers.org/) 在测试中启动容器，
  本地与 CI 运行测试都需要可用的 Docker 环境：
  - `quartz-sample`：`mysql:26.7.0-oraclelinux9`
  - `shardingsphere-jdbc-4.x-spring-sample`：`mysql:8`（全仓库统一 testcontainers 2.0.5；
    `spring-boot-2` 另经 `jackson-bom:2.20.1` 锁定 Jackson 全家、显式升级 `commons-lang3:3.17.0`，
    避免与 Boot 2.7 的旧版本偏斜。
    且 Boot 2.7.x 管理的 `mysql-connector-j:8.0.33` 无法握手 MySQL 26，因此用 8.x 大版本镜像而非
    `core-sample` 的 `26.7.0`。另注意 ShardingSphere 5.2.1/4.1.1 的 JDBC URL 解析器不支持 tag 中带
    `.`/`-` 的 tc URL，故不能用 `8.4.7-oraclelinux9` 这类精确 tag）
  - `shardingsphere-jdbc-5.x-spring-sample`：`mysql:8`（同上）
  - `tx-local-sample`：`postgres:18.6-trixie`（测试资源里用 `init-schemas.sql` 预建 schema）
  - `tx-seata-sample`：`postgres:18.6-trixie` + `apache/seata-server:2.6.0`（与客户端版本对齐）
  - `config-consul-sample`：`hashicorp/consul:2.0.4`（向 KV 写入 `dynamic-datasource/application/data`）
  - `config-nacos-sample`：`nacos/nacos-server:v3.2.4`（新版 starter 内含 3.x 客户端，走 gRPC，
    发布走 v3 运维 API，测试按实际映射端口动态计算 `nacos.server.grpc.port.offset`）
  - `shardingsphere-jdbc-5.x-core-sample`：`mysql:26.7.0-oraclelinux9`
- `spring-boot-2` 下模块的主代码保持 Java 8 兼容，配套使用 testcontainers 1.x（2.x 要求 Java 17+），
  因此 JDK 8 的 CI 任务直接对 `shardingsphere-jdbc-4.x-spring-sample` 与 `shardingsphere-jdbc-5.x-spring-sample` 执行 `clean test`；
  JDK 17/21/25/27 的全量 `clean test` 同样覆盖这两个模块。

## GraalVM Native Image

native image 的构建与测试、Native Test CI 的模块矩阵、Reachability Metadata 写法与上游 issue 跟进见
[docs/graalvm-native-image.md](./docs/graalvm-native-image.md)。

## Rust / TypeScript / Python 示例（`polyglot/`）

`polyglot/dynamic-datasource-ffi` 是普通的 Maven 模块，按 Java 25 编译。根 pom 的 `polyglot` profile 按 JDK 版本自动激活：
在 JDK 25+ 上执行上面的 `./mvnw -T1C -e clean test` 会包含它的 JVM 单测（H2 与 Testcontainers 的 PostgreSQL），
JDK 17、21 上则跳过整个 `polyglot/`；Lint & Format 也因此在 JDK 25 上执行。三个语言示例另有各自的工具链：CI 的
`Lint & Format CI` 检查它们的 license 头、格式与 lint；`Polyglot CI` 的三个 job 与本地一样，由各示例的构建先用
GraalVM 构建共享库（见 `polyglot/build-libdynamicds.sh`），再跑集成测试（先用 H2，再用各自语言的 Testcontainers
启动的 PostgreSQL）。本地验证二选一：

- 本机装有 GraalVM 25、Docker 与对应语言的工具链（Rust 1.88+、uv、Node.js 26.9+），或者在上文的 Dev Container 里时，
  在仓库根目录执行；共享库会在第一次构建时自动构建，之后只在 Java 源码或 pom 变化时重新构建（rootless Docker 下，
  Rust 与 Python 的测试还需要 `export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/docker.sock`）：

  ```shell
  (cd polyglot/rust-axum-sample && cargo fmt --check && cargo clippy --all-targets -- -D warnings && cargo test)
  (cd polyglot/python-fastapi-sample && uv run ruff format --check . && uv run ruff check . && uv run pytest)
  (cd polyglot/typescript-hono-sample && npm ci && npm run format:check && npm run typecheck && npm test)
  ```

- 只有 Docker 时，下面的命令在镜像构建时做格式检查与 lint，再在容器里跑同样的测试
  （rootless Docker 要加上 `DOCKER_SOCKET=$XDG_RUNTIME_DIR/docker.sock`）：

  ```shell
  docker compose -f polyglot/compose.yaml run --build --rm rust-test
  docker compose -f polyglot/compose.yaml run --build --rm python-test
  docker compose -f polyglot/compose.yaml run --build --rm typescript-test
  ```

修改 HTTP 接口时三个示例要同步修改，并保持 [polyglot/README.md](./polyglot/README.md) 中的约定一致。
数据源配置与 SQL 脚本在三个示例与 `dynamic-datasource-ffi/src/test/resources` 下各有一份，想让改动对所有示例生效时要逐份修改。

## 代码风格（Lint & Format）

CI 的 `Lint & Format CI` 任务在 JDK 25 下执行格式与静态检查，本地提交前请在**仓库根目录**执行：

```shell
# 格式化 Java 代码
./mvnw spotless:apply
# 校验格式
./mvnw spotless:check
# 静态检查
./mvnw checkstyle:check
# 补齐 / 校验 license 头（Java 源码，以及 .devcontainer/、config/license/、polyglot/ 下除 JSON、Markdown、lock 文件外的所有文件）
./mvnw license:format
./mvnw license:check
```

- 格式化使用 [Spotless](https://github.com/diffplug/spotless) + `google-java-format` 的 AOSP 风格（4 空格缩进），
  对 `src/main/java` 与 `src/test/java` 生效；通配符 `*` 导入会被 Checkstyle 拦截，需要手工展开。
- 静态检查使用 [Checkstyle](https://checkstyle.org/) 的 `google_checks.xml`。
  与格式化冲突（缩进、导入顺序等）或示例项目不适用的规则在 [`config/checkstyle/suppressions.xml`](./config/checkstyle/suppressions.xml) 中有意抑制，
  新增抑制项请附带原因注释。
- 两个工具都要求 JDK 21+ 运行（`google-java-format` 1.30+ 与 Checkstyle 14 的要求），在 JDK 25+ 上运行才会检查到
  `polyglot/`；其余构建（如 JDK 8/17 下的 `clean test`）不受影响。

## 已发布构件的验收（`Verify released artifact`）

本仓库的 `ds.version`（根 `pom.xml`）指向的是**已发布到 Maven Central 的构件**，因此天然滞后于主仓库
一个发版周期。为把滞后期压到最短，新增了 [`.github/workflows/verify-release.yml`](./.github/workflows/verify-release.yml)，
它有三种触发方式，互为兜底：

1. **主仓库发布成功后自动触发**：`baomidou/dynamic-datasource` 的 `release.yml` 在发布完成后通过
   `repository_dispatch`（事件 `ds-released`）把新版本号推过来。这需要主仓库配置 Secret
   `SAMPLES_DISPATCH_TOKEN`——一个对 `dynamic-datasource/dynamic-datasource-samples` 有写权限的 PAT
   （classic PAT 勾 `repo`，或 fine-grained 给 Contents: Read and write）。
2. **每日定时兜底**：即使 dispatch 丢失或 PAT 过期，定时任务也会从 Central 的 `maven-metadata.xml`
   取最新 `<release>`，与 `ds.version` 比对，落后时才跑。
3. **手工触发**：Actions 页面 `Run workflow`，填入要验收的版本号。

流水线行为：

- 先轮询 Central 直到该版本的 `.pom` 可取（最长 30 分钟）——`Central Portal` 发布成功不等于
  `repo1.maven.org` 立刻可见，不等待会把"还在同步"误报成"构件不可用"。
- 用 `-Dds.version=<版本>` 覆盖（用户属性优先于 pom 里的同名 property），跑 JDK 17/21/25/27 全量
  `clean test`，外加 JDK 8 下的两个 `spring-boot-2` 模块。测试涉及 Testcontainers，需要可用的 Docker。
- **全部通过且版本比 pom 新**：自动开 PR 把 `ds.version` 推进到该版本（分支
  `chore/bump-ds-<版本>`，重复运行复用同一分支，不会堆出重复 PR）。**合并这个 PR 才是"承认该版本可用"。**
- **失败**：自动开 issue 报警。此时**不要**合并自动 bump PR，先判断是框架缺陷还是环境抖动
  （如 Testcontainers 拉镜像失败），后者重跑该 run 确认。

注意：本地想对着某个已发布版本验证，直接 `./mvnw -Dds.version=4.6.0 -T1C -e clean test` 即可，不必改 `pom.xml`。
