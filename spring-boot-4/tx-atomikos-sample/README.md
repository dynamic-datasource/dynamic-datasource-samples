# tx-atomikos-sample

基于 `dynamic-datasource-spring-boot4-starter` + MyBatis + Atomikos XA 的跨数据源分布式事务示例。
示例把订单（order）与账户（account）拆到两个数据源中，下单流程在一个 JTA 事务里跨库写入，
任意一步失败时由 Atomikos 以两阶段提交（2PC）保证两个库一起回滚。

本模块暂不支持 GraalVM native image：Atomikos 的 JTA/XA 在 native image 下还需要额外的
reachability metadata，因此未纳入 Native Test CI，与 tx-seata-sample 同属先例，
详见 [GraalVM Native Image](../../docs/graalvm-native-image.md)。

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Spring Boot | 4.1.1 |
| dynamic-datasource-spring-boot4-starter | 4.7.0（取自根 `pom.xml` 的 `ds.version`） |
| Atomikos（`transactions-jdbc` / `transactions-jta`） | 6.0.1 `jakarta` classifier，版本取自父模块 `spring-boot-4-samples` 的 `atomikos.version` |
| H2 | 由 Spring Boot BOM 管理，XA 数据源为 `org.h2.jdbcx.JdbcDataSource` |

## 业务流程

入口是 `OrderService#placeOrder`：

1. `@DS("order")` 切到订单数据源，`UserTransaction.begin()` 开启 JTA 事务，插入 `orders`；
2. `AccountService#deduct` 以 `@DS("account")` 切到账户数据源，余额不足时影响行数为 0，
   抛出 `InsufficientBalanceException`；
3. `UserTransaction.commit()` 提交，Atomikos 对 order、account 两个 XA 资源做两阶段提交；
   任一步抛异常则 `rollback()`，两个库一起回滚。

## 关键配置

```yaml
spring:
  datasource:
    dynamic:
      primary: order
      atomikos:                      # 全局连接池参数，可在单个数据源下用同样的 key 覆盖
        min-pool-size: 1
        max-pool-size: 10
        borrow-connection-timeout: 30
        max-idle-time: 60
        maintenance-interval: 60
      datasource:
        order:
          # type 必须显式声明：Hikari 创建器优先级更高，type 缺省会接管并把 XA 数据源类
          # 误当作 java.sql.Driver 加载而启动失败。
          type: com.atomikos.jdbc.AtomikosDataSourceBean
          # driver-class-name 指向 XA 数据源类（不是普通 JDBC Driver）
          driver-class-name: org.h2.jdbcx.JdbcDataSource
          url: jdbc:h2:mem:order;DB_CLOSE_DELAY=-1
          username: sa
          password: ""
        account:
          type: com.atomikos.jdbc.AtomikosDataSourceBean
          driver-class-name: org.h2.jdbcx.JdbcDataSource
          url: jdbc:h2:mem:account;DB_CLOSE_DELAY=-1
          username: sa
          password: ""
```

换成生产数据库时只需替换 `driver-class-name` 与连接信息：MySQL 用
`com.mysql.cj.jdbc.MysqlXADataSource`，PostgreSQL 用 `org.postgresql.xa.PGXADataSource`。

内置支持的 XA 数据源类（`XADataSourceEnum`）：

| 数据库 | XA 数据源类 |
| --- | --- |
| MySQL 8+ | `com.mysql.cj.jdbc.MysqlXADataSource` |
| MySQL 5.1 | `com.mysql.jdbc.jdbc2.optional.MysqlXADataSource` |
| Oracle | `oracle.jdbc.xa.client.OracleXADataSource` |
| PostgreSQL | `org.postgresql.xa.PGXADataSource` |
| H2 | `org.h2.jdbcx.JdbcDataSource` |

其他数据库的 XA 数据源类不在枚举内，不会被 Atomikos 创建器认领，需要自定义 `DataSourceCreator`。

Atomikos 连接池参数（`spring.datasource.dynamic.atomikos.*`，单数据源下可用同名 key 覆盖）：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `min-pool-size` | 1 | 最小连接数 |
| `max-pool-size` | 10 | 最大连接数 |
| `borrow-connection-timeout` | 30 | 池空时等待连接的秒数 |
| `reap-timeout` | 0 | 连接最大借用秒数；**Atomikos 6 已移除该配置**，4/5 生效 |
| `max-idle-time` | 60 | 超出 min-pool-size 的空闲连接保留秒数 |
| `test-query` | `SELECT 1` | 归还连接前的验证语句 |
| `maintenance-interval` | 60 | 池维护线程间隔秒数 |
| `default-isolation-level` | -1 | 默认隔离级别，负值忽略 |
| `max-lifetime` | 60 | 连接最大存活秒数，0 为不限制 |

## 四个必须手工处理的点

1. **Boot 4 不再自动装配 Atomikos 的 JTA 事务管理器**（Boot 3 及以前会自动装配），
   需要自行声明 `UserTransactionManager` 与 `JtaTransactionManager`，见
   `config/JtaConfig`；同时 Atomikos 的 `transactions-jta` 把 `jakarta.transaction-api`
   声明为 provided，不会随依赖传递，需要显式引入，否则事务管理器会回退成
   `JdbcTransactionManager`，XA 根本不会发生。
2. **多数据源 + MyBatis 不要用 Spring 的 `@Transactional`**：MyBatis-Spring 在 Spring 事务
   同步激活时会把整个事务复用同一个 SqlSession（连同同一个连接），连接按事务内第一条语句的
   `@DS` 确定数据源，后续 `@DS` 切换全部失效（表现为第二个库 "Table not found"）。
   本示例用编程式 JTA：

   ```java
   @DS("order")
   public void placeOrder(PlaceOrderRequest request) {
       try {
           userTransactionManager.begin();
           orderMapper.insert(order);                           // -> order 数据源
           accountService.deduct(request.getUserId(), amount);   // @DS("account") -> account 数据源
           userTransactionManager.commit();                     // 两个 XA 资源 2PC
       } catch (RuntimeException e) {
           userTransactionManager.rollback();                   // 两个库一起回滚
           throw e;
       } catch (Exception e) {
           userTransactionManager.rollback();
           throw new IllegalStateException("JTA 事务提交失败", e);
       }
   }
   ```

   完整说明见 `service/OrderService` 的类注释。
3. **MyBatis 侧使用 `AtomikosTransactionFactory`**（`config/MyBatisConfig`）：它在每次 MyBatis
   事务开始时先按当前 `@DS` 确定真实数据源，再交给 `SpringManagedTransaction`。
4. **XA 数据源不要用 `datasource.*.init.schema` 建表**：脚本在 XA 连接池取出的连接上执行，
   Atomikos 在事务管理器未运行时会话不提交，连接关闭时未提交的 DDL 会被回滚。本示例的
   H2 建表在 `config/H2SchemaInitializer` 中用普通连接完成；真实项目请直接连库执行 DDL，
   或交给 Flyway / Liquibase。

## 运行方式

```bash
# 单元测试（H2 内存库，无需 Docker）
./mvnw -pl spring-boot-4/tx-atomikos-sample test

# 启动应用，然后 curl -X POST http://localhost:8080/order \
#   -H 'Content-Type: application/json' -d '{"userId":1,"amount":30.00}'
./mvnw -pl spring-boot-4/tx-atomikos-sample spring-boot:run
```

初始化数据由 `db/h2/schema-*.sql` 预置：`account(user_id = 1)` 的 `balance = 100.00`。

## 常见问题

- **`NoSuchMethodError: setReapTimeout(int)`**：Atomikos 6.0.0 起（jakarta 版本线）移除了
  `setReapTimeout`，dynamic-datasource 4.6.0 及更早版本在 Atomikos 6 下启动即失败
  （[baomidou/dynamic-datasource#822](https://github.com/baomidou/dynamic-datasource/issues/822)），
  需升级到包含该修复的版本（预计 4.7.0）。
- **Atomikos 6 与 Spring Boot 3/4**：必须使用 `jakarta` classifier 的
  `transactions-jdbc` / `transactions-jta`；Atomikos 5 及更早只有 `javax` 构件。
- **`uniqueResourceName` 冲突**：Atomikos 要求每个 XA 资源名唯一，dynamic-datasource 会自动
  取数据源的 key 作为 `uniqueResourceName`，手工添加数据源时注意不要重名。
- **事务日志**：Atomikos 会在工作目录写 `transaction-logs`，异常退出后重启会先做故障恢复，
  属预期行为。
