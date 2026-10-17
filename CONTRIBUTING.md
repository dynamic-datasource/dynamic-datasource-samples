# Contributing

我们欢迎社区的贡献。围绕此 git 的讨论与协作应通过 https://github.com/baomidou/dynamic-datasource 进行。
在提交 Pull Request 之前, 请在本地通过 [OpenJDK 17, OpenJDK 27] 的 JDK 范围下完成此命令的验证。
我们鼓励通过 `version-fox/vfox` 切换到 `25.3.4.1-graalce` 来验证。

针对 `JetBrains/intellij-community` 等 IDE，`spring-boot-2` 下模块的语言级别应设置为 JDK 8，
`spring-boot-3` 与 `spring-boot-4` 下模块的语言级别应设置为 JDK 17。 下文讨论不同情况下可能的测试流程，

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

native image 的构建与测试、Native Test CI 分片、Reachability Metadata 写法与上游 issue 跟进见
[docs/graalvm-native-image.md](./docs/graalvm-native-image.md)。

## 代码风格（Lint & Format）

CI 的 `Lint & Format CI` 任务在 JDK 21 下执行格式与静态检查，本地提交前请在**仓库根目录**执行：

```shell
# 格式化 Java 代码
./mvnw spotless:apply
# 校验格式
./mvnw spotless:check
# 静态检查
./mvnw checkstyle:check
```

- 格式化使用 [Spotless](https://github.com/diffplug/spotless) + `google-java-format` 的 AOSP 风格（4 空格缩进），
  对 `src/main/java` 与 `src/test/java` 生效；通配符 `*` 导入会被 Checkstyle 拦截，需要手工展开。
- 静态检查使用 [Checkstyle](https://checkstyle.org/) 的 `google_checks.xml`。
  与格式化冲突（缩进、导入顺序等）或示例项目不适用的规则在 [`config/checkstyle/suppressions.xml`](./config/checkstyle/suppressions.xml) 中有意抑制，
  新增抑制项请附带原因注释。
- 两个工具都要求 JDK 21+ 运行（`google-java-format` 1.30+ 与 Checkstyle 14 的要求），
  其余构建（如 JDK 8/17 下的 `clean test`）不受影响。

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
