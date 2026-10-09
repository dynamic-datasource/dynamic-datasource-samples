# 演示例子

大部分数据库连接为 `h2database` 或通过 `testcontainers-java` 启动的 `mysql-server`，仅供测试。

示例按 Spring Boot 版本分别存放在 `spring-boot-2`、`spring-boot-3`、`spring-boot-4` 文件夹下，
每个文件夹都是独立的 Maven 聚合模块（版本对应的 `dynamic-datasource-spring-boot-starter` /
`dynamic-datasource-spring-boot3-starter` / `dynamic-datasource-spring-boot4-starter` 见其下的 `pom.xml`）：

- `spring-boot-2` 下的子模块运行在 Spring Boot 2.7.x，可在 [OpenJDK 8, OpenJDK 21] 的 JDK 范围及其下游发行版下执行单元测试
- `spring-boot-3`（`springboot3-sample`）与 `spring-boot-4` 下的子模块分别运行在 Spring Boot 3.5.x / 4.0.x，
  需要在 [OpenJDK 17, ...] 的 JDK 范围及其下游发行版下执行单元测试

所有单元测试在 Github Actions 完成验证。你可能希望参考 [位于 Github Actions 的 CI 文件](./.github/workflows/ci.yml)。

## Spring Boot 4（`spring-boot-4/`）

- add-remove-datasource-sample 动态添加删除数据源的使用示例
- beetlsql-sample 集成 BeetlSQL 的使用示例
- config-consul-sample 集成 Consul 配置中心的使用示例
- config-nacos-sample 集成 Nacos 配置中心的使用示例（按官方文档使用 `com.alibaba.cloud:spring-alibaba-nacos-config`）
- druid-sample 集成mybatis和druid的使用示例
- jdbc-template-sample 集成原生jdbcTemplate的使用示例
- load-datasource-from-jdbc-sample 通过数据库配置来启动数据源示例
- mybatis-sample 集成原生mybatis的使用示例
- mybatisplus3-sample 集成mybatisPlus3的使用示例
- name-pattern-sample 自定义切面的使用示例
- quartz-sample 多数据源集成quartz示例
- shardingsphere-jdbc-5.x-core-sample 集成 ShardingSphere JDBC Driver 5.5.2 使用示例
- spel-sample 动态从外部参数spel来切换数据源的使用示例
- tx-local-sample 本地事务示例项目★★★★★★必看★★★★★★
- tx-seata-sample 基于seata的分布式事务集成使用示例

## Spring Boot 3（`spring-boot-3/`）

- springboot3-sample Spring Boot 3 使用示例

## Spring Boot 2（`spring-boot-2/`）

- shardingsphere-jdbc-4.x-spring-sample 集成 ShardingSphere JDBC Spring Boot Starter 4.1.1 使用示例, 不再维护，
  参考 https://github.com/apache/shardingsphere/releases/tag/5.0.0-alpha
- shardingsphere-jdbc-5.x-spring-sample 集成 ShardingSphere JDBC Spring Boot Starter 5.2.1 使用示例，不再维护，
  参考 https://github.com/apache/shardingsphere/issues/22469

## 单元测试说明

- 纯 H2 的模块（`mybatisplus3-sample`、`beetlsql-sample`、`jdbc-template-sample`、`name-pattern-sample`、
  `add-remove-datasource-sample`、`load-datasource-from-jdbc-sample`、`druid-sample`、`spel-sample`、
  `shardingsphere-jdbc-4.x-spring-sample`、`shardingsphere-jdbc-5.x-spring-sample`）可直接 `mvn test`，无需外部服务。
- 涉及外部中间件的模块使用 [Testcontainers](https://java.testcontainers.org/) 在测试中启动容器，
  本地与 CI 运行测试都需要可用的 Docker 环境：
  - `quartz-sample`：`mysql:8.4.7-oraclelinux9`
  - `tx-local-sample`：`postgres:16-alpine`（测试资源里用 `init-schemas.sql` 预建 schema）
  - `tx-seata-sample`：`postgres:16-alpine` + `apache/seata-server:2.6.0`（与客户端版本对齐）
  - `config-consul-sample`：`hashicorp/consul:1.15`（向 KV 写入 `dynamic-datasource/application/data`）
  - `config-nacos-sample`：`nacos/nacos-server:v3.2.4`（新版 starter 内含 3.x 客户端，走 gRPC，
    发布走 v3 运维 API，测试按实际映射端口动态计算 `nacos.server.grpc.port.offset`）
  - `shardingsphere-jdbc-5.x-core-sample`：`mysql:8.4.7-oraclelinux9`
- `spring-boot-2` 下模块的主代码保持 Java 8 兼容，配套使用 testcontainers 1.x（2.x 要求 Java 17+），
  因此 JDK 8 的 CI 任务直接对 `shardingsphere-jdbc-4.x-spring-sample` 与 `shardingsphere-jdbc-5.x-spring-sample` 执行 `clean test`；
  JDK 17/21 的全量 `clean test` 同样覆盖这两个模块。

## Contributing

我们欢迎社区的贡献。围绕此 git 的讨论与协作应通过 https://github.com/baomidou/dynamic-datasource/issues 进行。
在提交 Pull Request 之前, 请在本地通过 [OpenJDK 17, OpenJDK 21] 的 JDK 范围下完成此命令的验证。
我们鼓励通过 `SDKMAN!` 或 `version-fox/vfox` 切换到 `21.0.2-graalce` 来验证。

针对 IntelliJ IDEA 等 IDE，`spring-boot-2` 下模块的语言级别应设置为 JDK 8，`spring-boot-3` 与 `spring-boot-4` 下模块的语言级别应设置为 JDK 17。
下文讨论不同情况下可能的测试流程，

### Ubuntu 26.04.1

假设 `SDKMAN!` 和 `Docker Engine` 已安装，在 Bash 执行如下命令，

```shell
sdk install java 21.0.2-graalce
sdk use java 21.0.2-graalce

git clone git@github.com:dynamic-datasource/dynamic-datasource-samples.git
cd ./dynamic-datasource-samples/
./mvnw -T1C -e clean test
```

### Windows 11

假设 `PowerShell/PowerShell`, `version-fox/vfox`, `git-for-windows/git` 和 `rancher-sandbox/rancher-desktop` 已安装，
在 `PowerShell 7` 执行如下命令，

```shell
rdctl start --application.start-in-background --container-engine.name=moby --kubernetes.enabled=false
vfox add java
vfox install java@21.0.2-graalce
vfox use --global java@21.0.2-graalce

git clone git@github.com:dynamic-datasource/dynamic-datasource-samples.git
cd ./dynamic-datasource-samples/
./mvnw -T1C -e clean test
```
