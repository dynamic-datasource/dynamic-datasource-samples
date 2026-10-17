# 上游 issue 跟进（TODO）

依赖上游修复的待办。每条以 `TODO` 开头，写明上游 issue 与修复后本仓库要做的事；背景见
[graalvm-native-image.md](./graalvm-native-image.md)。

- TODO https://github.com/apache/incubator-seata/issues/8303 ：Seata 修复 AOT 问题后，
  把 `tx-seata-sample` 纳入 native CI，并用 nativeTest 复核按 JVM agent 裁剪的
  `seata-all` 条目（agent 另采到 111 个尚未登记的类，届时按报错补）。
- TODO https://github.com/spring-projects/spring-framework/issues/37500 ：Spring AOT
  保留 `autowireMode` 后，去掉各模块 `@MapperScan` 的 `sqlSessionFactoryRef`
  （逐模块去属性并跑 nativeTest 验证）。
- TODO https://github.com/mybatis/spring/issues/1309 ：mybatis-spring 内置 mapper 的 AOT
  处理器（为 mapper 接口注册反射、JDK 代理与同名 XML 等 hints）后，升级父 POM 的
  `mybatis-spring.version`，删除 Boot 4 各模块的 `MyBatisNativeConfiguration` 并逐模块跑
  nativeTest 验证；`springboot3-sample` 不必等此项：MP 3.5.17 起自带
  `mybatis-plus-spring-boot-native-image`，升级 MP 后即可删除其副本。
- TODO https://github.com/baomidou/dynamic-datasource/issues/809 ：DD 补上
  `@NestedConfigurationProperty` 或自带元数据后，删除各模块
  `dynamic-datasource-creator` 目录下的 `DatasourceInitProperties` 条目。
- TODO https://github.com/baomidou/dynamic-datasource/issues/811 ：DD 把 mybatis-spring
  改为 `compileOnly` 后，父 POM 对 mybatis-spring 的钉版本只剩统一 MP 模块（MP starter
  自带 4.0.0）的作用，届时再决定去留，改动后跑 JVM 与 nativeTest 验证。
- TODO https://github.com/oracle/graal/issues/14708 ：GraalVM parser 接受 `reason`
  字段、不再报 Unknown attribute 后，确认各模块构建日志无新增警告。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/542 ：官方收录
  `seata-all` 后，对照精简 `tx-seata-sample` 的 `org.apache.seata/seata-all` 条目
  （验证需等 apache/incubator-seata#8303 修复、该模块能跑 nativeTest）。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/9432 ：官方补测
  `mysql-connector-j` 8.0.31 元数据（覆盖到 9.7.0）；关闭后升级 native-maven-plugin
  （官方元数据随插件版本发布），若已含 `Driver` 与 `TimeZoneMapping.properties`，删除
  `shardingsphere-jdbc-5.x-core-sample` 的 `com.mysql/mysql-connector-j` 目录并跑
  nativeTest 验证。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/9437 ：官方补测
  `mybatis-plus` 3.5.3 元数据（覆盖到 3.5.17，目前是空的 `{}`）；关闭后同样升级插件，
  若已含 `MybatisXMLLanguageDriver` 与 `CompositeEnumTypeHandler`，删除各模块
  `com.baomidou/mybatis-plus` 目录并跑 nativeTest 验证。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/10906 ：官方补上
  caffeine 的 `SIMS`（`maximumSize` + `softValues`）后升级插件，删除
  `shardingsphere-jdbc-5.x-core-sample` 的 `com.github.ben-manes.caffeine` 目录并跑
  nativeTest 验证。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/10907 ：官方补上
  OGNL 用到的 `SystemMetaObject`/`MetaObject` 与 `EnumTypeHandler(Class)` 构造器后升级
  插件，删除 `mybatisplus3-sample`、`tx-local-sample` 的 `org.mybatis/mybatis` 目录并跑
  nativeTest 验证（`tx-seata-sample` 的同类目录待 Seata 修复后再动）。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/10908 ：官方让
  `tables_*.sql` 在 Spring Boot 建表初始化时已可用后升级插件，删除 `quartz-sample` 的
  `org.quartz-scheduler` 目录并跑 nativeTest 验证。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/10909 ：官方收录
  `dynamic-datasource-creator` 4.6.0 后升级插件，删除各模块
  `com.baomidou/dynamic-datasource-creator` 目录并跑 nativeTest 验证（与上面 #809
  哪个先落地都行）。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/10910 ：官方收录
  `mybatis-spring` 4.1.0 后升级插件，删除各模块 `org.mybatis/mybatis-spring` 目录并跑
  nativeTest 验证。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/10911 ：官方收录
  `spring-cloud-consul-core` 5.0.3 后升级插件，删除 `config-consul-sample` 的
  `org.springframework.cloud` 目录并跑 nativeTest 验证。
- TODO https://github.com/oracle/graalvm-reachability-metadata/issues/10912 ：官方收录
  `p6spy` 3.9.1 后升级插件，删除 `druid-sample` 的 `p6spy` 目录并跑 nativeTest 验证。
- TODO ShardingSphere 5.5.3 自带元数据（`shardingsphere-infra-reachability-metadata`）的问题：
  SnakeYAML 反射构造的 7 个 YAML 配置类用的是自条件、不生效，`YamlSingleRuleConfiguration`
  与 8 个 MySQL 解析器 SPI 类完全缺失；跟进地址 https://github.com/apache/shardingsphere/issues ，
  目前暂未提 issue（提到 GRM 会让机器人为 65 个未收录的传递依赖各开一个 issue，故暂缓）。
  修复后精简 `shardingsphere-jdbc-5.x-core-sample` 的 `org.apache.shardingsphere` 目录并跑
  nativeTest 验证；其中条件为 `YamlEngine` 等的 4 条可能已与自带元数据重复，届时一并验证。
- TODO BeetlSQL 的 native 问题（`sqlManager` 的 `MySqlStyle` 不支持 AOT 值序列化），
  跟进地址 https://gitee.com/xiandafu/beetlsql/issues ，目前暂无已知 issue；
  问题修复后把 `beetlsql-sample` 纳入 native CI，并用 nativeTest 复核其元数据。
