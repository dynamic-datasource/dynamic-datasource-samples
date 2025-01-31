# 演示例子

大部分数据库连接为 H2Database，仅供测试。

除开 `com.baomidou:springboot3-sample`， 其他子模块均可在 [OpenJDK 8, OpenJDK 21] 的 JDK 范围及其下游发行版下执行单元测试。

所有单元测试在 Github Actions 完成验证。你可能希望参考 [位于 Github Actions 的 CI 文件](./.github/workflows/ci.yml)。

- add-remove-datasource 动态添加删除数据源的使用示例
- all-datasource-sample 所有不同连接池使用示例（大乱炖，实际不建议）
- druid-sample 集成mybatis和druid的使用示例
- jdbc-template-sample 集成原生jdbcTemplate的使用示例
- load-datasource-from-jdbc 通过数据库配置来启动数据源示例
- mybatis-sample 集成原生mybatis的使用示例
- mybatisplus2-sample 集成mybatisPlus2的使用示例
- mybatisplus3-sample 集成mybatisPlus3的使用示例
- name-pattern-sample 自定义切面的使用示例
- nest-sample 嵌套切换数据源使用示例
- quartz-sample 多数据源集成quartz示例
- shardingsphere-jdbc-4.x-spring-sample 集成 ShardingSphere JDBC Spring Boot Starter 4.1.1 使用示例, 不再维护,
  参考 https://github.com/apache/shardingsphere/releases/tag/5.0.0-alpha
- shardingsphere-jdbc-5.x-core-sample 集成 ShardingSphere JDBC Driver 5.5.2 使用示例
- shardingsphere-jdbc-5.x-spring-sample 集成 ShardingSphere JDBC Spring Boot Starter 5.2.1 使用示例, 不再维护,
  参考 https://github.com/apache/shardingsphere/issues/22469
- spel-sample 动态从外部参数spel来切换数据源的使用示例
- tx-local-sample 本地事务示例项目★★★★★★必看★★★★★★
- tx-seata-sample 基于seata的分布式事务集成使用示例

## Contributing

我们欢迎社区的贡献。围绕此 git 的讨论与协作应通过 https://github.com/baomidou/dynamic-datasource/issues 进行。

针对 IDE，项目的语言级别应设置为 JDK 8，对于单独的 `com.baomidou:springboot3-sample` 子模块，语言级别应设置为 JDK 17 。
在提交 Pull Request 之前, 请在本地通过 [OpenJDK 17, OpenJDK 21] 的 JDK 范围下完成此命令的验证。
我们鼓励通过 `SDKMAN!` 切换到 `21.0.2-graalce` 来验证。

```shell
sdk install java 21.0.2-graalce
sdk use java 21.0.2-graalce

git clone git@github.com:dynamic-datasource/dynamic-datasource-samples.git
cd ./dynamic-datasource-samples/
./mvnw -T1C -e clean test
```
