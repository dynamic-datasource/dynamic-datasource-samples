# tx-seata-sample

基于 `dynamic-datasource-spring-boot4-starter` + MyBatis-Plus + Seata AT 模式的跨数据源分布式事务示例。
示例把订单（order）、账户（account）、商品库存（product）拆分到多个数据源中，下单流程在一个全局事务里跨库写入，
任意一步失败时由 Seata 通过各库的 `undo_log` 整体回滚。

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Spring Boot | 4.1.1 |
| dynamic-datasource-spring-boot4-starter | 4.5.0 |
| Seata（`seata-spring-boot-starter` / `seata-server`） | 2.6.0，客户端与服务端版本对齐 |
| PostgreSQL（默认 profile） | `postgres:18.6-trixie` |
| MySQL（可选 profile） | `mysql:26.7.0-oraclelinux9` |

## 业务流程

入口是 `OrderServiceImpl#placeOrder`：

1. `@DS("order")` 切到订单数据源，`@GlobalTransactional` 开启全局事务，插入 `p_order`（状态 `INIT`）；
2. `ProductServiceImpl#reduceStock` 以 `@DS("product")` 切到商品数据源，校验并扣减库存，返回商品总价；
3. `AccountServiceImpl#reduceBalance` 以 `@DS("account")` 切到账户数据源，校验并扣减余额；
4. 回到订单数据源把订单状态更新为 `SUCCESS`。

各实现类会打印 `RootContext.getXID()`，一次成功下单在三个数据源的日志里应出现同一个 XID。

初始化数据由各 `schema-*.sql` 预置：

| 数据 | 初始值 |
| --- | --- |
| `account(id = 1)` | `balance = 50` |
| `product(id = 1)` | `price = 10`、`stock = 20` |

## 关键配置

### `application.yml`

```yaml
seata:
  enabled: true
  application-id: applicationName
  tx-service-group: my_test_tx_group
  enable-auto-data-source-proxy: false # 必须为 false，数据源代理交给 dynamic-datasource
  service:
    vgroup-mapping:
      # key 与上面的 tx-service-group 对应
      my_test_tx_group: default
    grouplist:
      # 示例使用 file 注册中心，seata-server 地址只能写在 grouplist 里
      default: 110.40.253.205:8091
  config:
    type: file
  registry:
    type: file
```

- `enable-auto-data-source-proxy` 必须为 `false`：数据源由 dynamic-datasource 创建并代理，Seata 只负责全局事务协调，开启会形成双重代理。
- `registry` / `config` 使用 `file` 时，客户端通过 `grouplist` 直连 Seata Server；改用 Nacos 等注册中心后可去掉 `grouplist`。

### `application-postgresql.yml` / `application-mysql.yml`

- `spring.datasource.dynamic.seata: true` 与 `seata-mode: AT`：默认所有动态数据源都参与 AT 模式代理，单个数据源可用 `seata: false` 关闭（示例中的 H2 `test` 数据源就不参与）。
- 每个参与 AT 事务的数据源都需要 `undo_log` 表：PostgreSQL 的 `db/postgresql/schema-*.sql` 已包含；MySQL 的 `db/mysql/schema-*.sql` 只建业务表，需要额外执行 `db/mysql/undo_log.sql`。
- PostgreSQL 用 `hikari.schema` 把连接切到 `seata_order` / `seata_account` / `seata_product`，应用启动时会执行 `init.schema` 建表并写入初始数据。脚本包含 `DROP TABLE`，每次启动都会重置业务表。

## 本地运行

以下命令均在仓库根目录执行。

### 1. 启动 Seata Server

```shell
docker run --name seata-server -v /etc/localtime:/etc/localtime -p 8091:8091 -d apache/seata-server:2.6.0
```

`8091` 是事务协调端口，与 `application.yml` 中 `grouplist.default` 的端口保持一致。

### 2. 启动数据库

默认使用 `postgresql` profile：

```shell
docker run --name seata-postgres -v /etc/localtime:/etc/localtime -p 5432:5432 \
  -e POSTGRES_PASSWORD=nknUPjQsY2uz6WST -e POSTGRES_DB=seata \
  -d postgres:18.6-trixie

docker exec -it seata-postgres psql -U postgres -d seata -c \
  "CREATE SCHEMA IF NOT EXISTS seata_order; CREATE SCHEMA IF NOT EXISTS seata_account; CREATE SCHEMA IF NOT EXISTS seata_product;"
```

也可以切换 `mysql` profile：

```shell
docker run --name seata-mysql -v /etc/localtime:/etc/localtime -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 -d mysql:26.7.0-oraclelinux9

docker exec -it seata-mysql mysql -uroot -p123456 -e \
  "CREATE DATABASE IF NOT EXISTS seata_order; CREATE DATABASE IF NOT EXISTS seata_account; CREATE DATABASE IF NOT EXISTS seata_product;"

# 每个参与 AT 事务的库都需要 undo_log 表
for db in seata_order seata_account seata_product; do
  docker exec -i seata-mysql mysql -uroot -p123456 "$db" \
    < spring-boot-4/tx-seata-sample/src/main/resources/db/mysql/undo_log.sql
done
```

### 3. 启动应用

把 `application-postgresql.yml` 或 `application-mysql.yml` 中的 `110.40.253.205` 换成本机地址（如 `localhost`），
然后从 IDE 运行 `SeataApplication`，或执行：

```shell
# 默认 postgresql profile
./mvnw -pl spring-boot-4/tx-seata-sample spring-boot:run

# mysql profile
./mvnw -pl spring-boot-4/tx-seata-sample spring-boot:run -Dspring-boot.run.profiles=mysql
```

启动后可访问 Swagger UI：<http://localhost:8080/swagger-ui.html>。

### 4. 调用接口验证

```shell
# 成功下单：1 号用户购买 1 号商品 2 件，总价 20，返回“下单成功”
curl -X POST http://localhost:8080/order/placeOrder \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"productId":1,"amount":2}'

# 库存不足回滚：一次购买 22 件，超过库存 20，期望 500 回滚
curl -X POST http://localhost:8080/order/test1

# 余额不足回滚：一次购买 6 件，总价 60 超过余额 50，期望 500 回滚
curl -X POST http://localhost:8080/order/test2
```

`test1`/`test2` 由 `GlobalExceptionHandler` 返回 500 和异常信息（`库存不足` / `余额不足`）。
回滚后再查数据库，订单表没有新增记录，库存与余额保持初始值。

## 单元测试

`SeataSampleTest` 使用 Testcontainers 启动 `postgres:18.6-trixie` 与 `apache/seata-server:2.6.0`，
并通过 `@DynamicPropertySource` 把三个数据源和 `seata.service.grouplist.default` 指向容器，覆盖两种场景：

- `placeOrderSuccess`：下单成功后订单状态为 `SUCCESS`，库存 20 → 18，余额 50 → 30；
- `placeOrderRollbackOnInsufficientBalance`：总价 100 超过余额 50 时抛出异常，三个数据源的数据全部回滚到初始状态。

```shell
./mvnw -pl spring-boot-4/tx-seata-sample -am test
```

测试依赖 Testcontainers，本地与 CI 运行都需要可用的 Docker 环境。
