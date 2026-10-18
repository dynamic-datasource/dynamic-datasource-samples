# 演示例子

示例按 Spring Boot 版本分别存放在 `spring-boot-2`、`spring-boot-3`、`spring-boot-4` 文件夹下，
每个文件夹都是独立的 Maven 聚合模块（版本对应的 `dynamic-datasource-spring-boot-starter` /
`dynamic-datasource-spring-boot3-starter` / `dynamic-datasource-spring-boot4-starter` 见其下的 `pom.xml`）：

- `spring-boot-2` 下的子模块运行在 Spring Boot 2.7.x，可在 [OpenJDK 8, OpenJDK 27] 的 JDK 范围及其下游发行版下执行单元测试
- `spring-boot-3`（`springboot3-sample`）与 `spring-boot-4` 下的子模块分别运行在 Spring Boot 3.5.x / 4.1.x，
  需要在 [OpenJDK 17, OpenJDK 27] 的 JDK 范围及其下游发行版下执行单元测试

另有 `polyglot` 文件夹，存放在 Rust / TypeScript / Python 的 web 项目中使用 dynamic-datasource 的示例。
其中的 Java 模块为 GraalVM 25 的 native image 而写，按 Java 25 编译，只在 [OpenJDK 25, OpenJDK 27] 的 JDK 范围下参与构建。

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
- tx-seata-sample：基于seata的分布式事务集成使用示例，Seata Server 与数据库的准备、关键配置和本地运行步骤见
  [模块 README](./spring-boot-4/tx-seata-sample/README.md)

## Spring Boot 3（`spring-boot-3/`）

- springboot3-sample：Spring Boot 3 使用示例

## Spring Boot 2（`spring-boot-2/`）

- shardingsphere-jdbc-4.x-spring-sample：集成 ShardingSphere JDBC Spring Boot Starter 4.1.1 使用示例，不再维护，
  参考 https://github.com/apache/shardingsphere/releases/tag/5.0.0-alpha
- shardingsphere-jdbc-5.x-spring-sample：集成 ShardingSphere JDBC Spring Boot Starter 5.2.1 使用示例，不再维护，
  参考 https://github.com/apache/shardingsphere/issues/22469

## Rust / TypeScript / Python（`polyglot/`）

dynamic-datasource 被构建成 GraalVM native 共享库，三个 web 示例通过各自语言的 FFI 在进程内调用它，运行时不需要 JVM。
数据源路由与分组负载均衡、按请求头切换、动态增删数据源、多数据源本地事务分别对应上面的 jdbc-template-sample、
spel-sample、add-remove-datasource-sample 与 tx-local-sample，H2 与 PostgreSQL 都可以运行。构建、运行、C ABI 与
线程模型见 [模块 README](./polyglot/README.md)：

- rust-axum-sample：在 Rust + axum 中使用的示例
- python-fastapi-sample：在 Python + FastAPI 中使用的示例
- typescript-hono-sample：在 TypeScript + Hono 中使用的示例（Node.js 26.9+ 内置的 `node:ffi`）

# Contributing

我们欢迎社区的贡献，详情请见 [CONTRIBUTING.md](./CONTRIBUTING.md)。
围绕此 git 的讨论请前往 https://github.com/baomidou/dynamic-datasource 。
