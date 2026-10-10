# 演示例子

示例按 Spring Boot 版本分别存放在 `spring-boot-2`、`spring-boot-3`、`spring-boot-4` 文件夹下，
每个文件夹都是独立的 Maven 聚合模块（版本对应的 `dynamic-datasource-spring-boot-starter` /
`dynamic-datasource-spring-boot3-starter` / `dynamic-datasource-spring-boot4-starter` 见其下的 `pom.xml`）：

- `spring-boot-2` 下的子模块运行在 Spring Boot 2.7.x，可在 [OpenJDK 8, OpenJDK 27] 的 JDK 范围及其下游发行版下执行单元测试
- `spring-boot-3`（`springboot3-sample`）与 `spring-boot-4` 下的子模块分别运行在 Spring Boot 3.5.x / 4.1.x，
  需要在 [OpenJDK 17, OpenJDK 27] 的 JDK 范围及其下游发行版下执行单元测试

所有单元测试与代码风格检查（Lint & Format）在 Github Actions 完成验证。你可能希望参考 [位于 Github Actions 的 CI 文件](./.github/workflows/ci.yml)。

## Spring Boot 4（`spring-boot-4/`）

- add-remove-datasource-sample：动态添加删除数据源的使用示例
- beetlsql-sample：集成 BeetlSQL 的使用示例
- config-consul-sample：集成 Consul 配置中心的使用示例
- config-nacos-sample：集成 Nacos 配置中心的使用示例（按官方文档使用 `com.alibaba.cloud:spring-alibaba-nacos-config`）
- druid-sample：集成mybatis和druid的使用示例
- jdbc-template-sample：集成原生jdbcTemplate的使用示例
- load-datasource-from-jdbc-sample：通过数据库配置来启动数据源示例
- mybatis-sample：集成原生mybatis的使用示例
- mybatisplus3-sample：集成mybatisPlus3的使用示例
- name-pattern-sample：自定义切面的使用示例
- quartz-sample：多数据源集成quartz示例
- shardingsphere-jdbc-5.x-core-sample：集成 ShardingSphere JDBC Driver 5.5.3 使用示例
- spel-sample：动态从外部参数spel来切换数据源的使用示例
- tx-local-sample：本地事务示例项目★★★★★★必看★★★★★★
- tx-seata-sample：基于seata的分布式事务集成使用示例

## Spring Boot 3（`spring-boot-3/`）

- springboot3-sample：Spring Boot 3 使用示例

## Spring Boot 2（`spring-boot-2/`）

- shardingsphere-jdbc-4.x-spring-sample：集成 ShardingSphere JDBC Spring Boot Starter 4.1.1 使用示例，不再维护，
  参考 https://github.com/apache/shardingsphere/releases/tag/5.0.0-alpha
- shardingsphere-jdbc-5.x-spring-sample：集成 ShardingSphere JDBC Spring Boot Starter 5.2.1 使用示例，不再维护，
  参考 https://github.com/apache/shardingsphere/issues/22469

# Contributing

我们欢迎社区的贡献。围绕此 git 的讨论与协作应通过 https://github.com/baomidou/dynamic-datasource/issues 进行。
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
