# DB Migrate

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-23-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk23-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)

`db-migrate` 是一个基于 Spring Boot 3.5 的轻量级异构数据迁移工具，当前聚焦于 MySQL 与 InfluxDB 之间的数据同步。项目使用策略模式、模板方法和适配器机制组织数据源实现，便于按现有结构继续扩展新的数据库类型。

## 核心特性

- 支持 `MySQL -> InfluxDB`、`InfluxDB -> MySQL` 以及同类型源/目标之间的数据迁移。
- 支持按库迁移和按表迁移两种模式。
- 按表迁移支持显式表清单、正则匹配表名、表名别名映射。
- 支持通过 `offset` 从指定偏移位置继续迁移，便于断点续跑或跳过历史数据。
- 支持分页拉取、批量写入和并行执行，适合中到大规模数据迁移场景。
- 支持全局字段忽略和 `null` 值过滤。
- Influx 目标端支持将指定字段写入时间列与 tag 列。
- MySQL 目标端支持按字段名自动创建基础表结构。
- 事务配置仅对 MySQL 目标端生效。

## 环境要求

- Java 23+
- Maven 3.9+
- MySQL 8.0+
- InfluxDB 3.x 兼容服务端

## 构建与运行

### 构建

```bash
mvn clean package
```

### 测试

```bash
mvn test
```

### 启动

```bash
mvn spring-boot:run
```

### 作为依赖接入

下游 Spring Boot 项目只需要：

1. 引入 `dbmig` 依赖。
2. 在自己的 `application.yml` 中配置 `mig.*`。
3. 将 `mig.on-start` 设为 `true`，应用启动后就会自动执行迁移。

项目已经通过 Spring Boot AutoConfiguration 自动注册 Bean，不需要下游额外编写 `@ComponentScan`、`@Import` 或手动创建执行器 Bean。

仓库中的本地调试配置文件 `migConf.yml` 不会被打进依赖包；请把真实配置写在下游应用自己的配置文件里。

## 配置说明

迁移统一使用 `mig.*` 前缀：

- `mig.*`：控制迁移模式、分页、事务、字段过滤以及当前任务使用的源/目标连接参数。
- `mig.db.source` / `mig.db.dest`：会根据 `source-type` / `dest-type` 自动映射到具体数据源属性对象。

### 最小示例：MySQL -> InfluxDB

```yaml
mig:
  on-start: true

  core:
    mode: table
    source-type: mysql
    dest-type: influx
    table:
      tables:
        - sensor_data
      pattern:
        - sensor_data_.*
      alias-map:
        sensor_data: sensor_metric
      offset: 1000

  db:
    source:
      host: localhost
      port: 3306
      database: source_db
      username: root
      password: your_password

    dest:
      host: localhost
      port: 8181
      database: metrics
      token: ${INFLUX_TOKEN}
      time-field: collectTime
      time-format: yyyy-MM-dd'T'HH:mm:ss
      tag-fields:
        pointId: pointId
        deviceCode: deviceCode

  pagination:
    enable: true
    size: 1000
    critical: 1000

  field:
    ignore:
      - createTime
      - updateTime
    ignore-null: false

  transaction:
    enable: false
```

### 最小示例：InfluxDB -> MySQL

```yaml
mig:
  on-start: true

  core:
    mode: db
    source-type: influx
    dest-type: mysql

  db:
    source:
      host: localhost
      port: 8181
      database: metrics
      token: ${INFLUX_TOKEN}

    dest:
      host: localhost
      port: 3306
      database: target_db
      username: root
      password: your_password

  field:
    ignore-null: false

  transaction:
    enable: true
    gran: TASK
```

## 关键配置项

### 通用迁移配置

| 配置项 | 说明 | 默认值 / 可选值 |
|:--|:--|:--|
| `mig.on-start` | 应用启动后是否立即执行迁移 | `false` |
| `mig.core.mode` | 迁移模式 | `db`、`table` |
| `mig.core.table.tables` | 按表迁移时显式指定的表名列表 | - |
| `mig.core.table.pattern` | 按表迁移时用于匹配源表名的正则列表 | - |
| `mig.core.table.alias-map` | 表名映射，格式为 `源表名:目标表名` | - |
| `mig.core.table.offset` | 从第几条记录之后开始迁移 | `0` |
| `mig.core.source-type` | 源数据源类型 | `mysql`、`influx` |
| `mig.core.dest-type` | 目标数据源类型 | `mysql`、`influx` |
| `mig.db.source` | 源端连接参数，字段结构随 `source-type` 变化 | - |
| `mig.db.dest` | 目标端连接参数，字段结构随 `dest-type` 变化 | - |
| `mig.pagination.enable` | 是否启用分页 | `true` 时启用分页逻辑 |
| `mig.pagination.size` | 分页大小 | 未配置时默认 `200` |
| `mig.pagination.critical` | 超过该记录数时启用分页 | 未配置时默认 `200` |
| `mig.field.ignore` | 全局忽略字段列表 | - |
| `mig.field.ignore-null` | 是否忽略值为 `null` 的字段 | `false` |
| `mig.transaction.enable` | 是否启用事务 | `false` |
| `mig.transaction.gran` | 事务粒度 | `PAGE`、`TASK` |

### MySQL 连接参数

| 配置项 | 说明 | 默认值 |
|:--|:--|:--|
| `host` | MySQL 主机地址 | `localhost` |
| `port` | MySQL 端口 | `3306` |
| `database` | 数据库名 | - |
| `username` | 用户名 | `root` |
| `password` | 密码 | - |

### Influx 连接参数

| 配置项 | 说明 | 默认值 |
|:--|:--|:--|
| `host` | Influx 主机地址 | `localhost` |
| `port` | Influx 端口 | `8181` |
| `database` | 数据库名 / bucket 标识 | `dbMig` |
| `token` | 访问令牌 | 读取环境变量 `INFLUX_TOKEN` |
| `time-field` | 将源记录中的哪个字段写为 Influx 时间列 | - |
| `time-format` | 自定义时间解析格式；未配置时按 `Instant.parse` 解析 | - |
| `tag-fields` | tag 字段映射，当前实现会读取映射的 `value` 作为 tag 字段名 | - |

## 表模式增强

当 `mig.core.mode=table` 时，当前实现支持三种选表方式，可混合使用：

- `tables`：直接指定要迁移的源表名。
- `pattern`：使用正则表达式从源端现有表中匹配目标表。
- `alias-map`：通过映射关系指定源表名和目标表名，写入目标端时会使用别名。

其中：

- 如果 `tables`、`pattern`、`alias-map` 都未配置，初始化会直接报错。
- `alias-map` 的 key 会被视为源表名，value 会被视为目标端表名或 measurement 名。
- `offset` 会写入迁移上下文，并作用于分页查询和全量查询，适合跳过已迁移数据。

示例：

```yaml
mig:
  core:
    mode: table
    table:
      tables:
        - orders
      pattern:
        - sensor_.*
      alias-map:
        orders: orders_archive
        sensor_data_2025: sensor_metric
      offset: 50000
```

## Influx 目标端行为

当目标端类型为 `influx` 时，`InfluxSource` 的写入行为如下：

- `time-field` 指定的字段不会作为普通 field 写入，而是会被提取为 Influx 记录时间。
- `time-format` 为空时，时间值必须能被 `Instant.parse(...)` 解析。
- `time-format` 已配置时，时间值会按 `DateTimeFormatter.ofPattern(time-format)` 解析，再按系统时区转换为 `Instant`。
- `tag-fields` 用于区分哪些列写入 tag；未命中的列继续作为普通 field 写入。
- 如果未配置 `tag-fields`，则所有业务列都按 field 写入。

## 架构概览

- `template`：定义不同数据源的查询、分页、写入和元数据操作。
- `adaptor`：在不同源数据对象之间做结构适配。
- `converter`：处理具体字段级转换逻辑。
- `properties`：负责迁移任务与数据源属性的配置绑定。
- `DatasourceInitializer` / `MigrateExecutor`：负责配置加载、数据源初始化和迁移执行。

## 注意事项

1. 项目当前编译目标为 Java 23，请确保本地 JDK 与 Maven 配置一致。
2. `migConf.yml` 是本地运行配置，不要提交真实数据库密码或 Token。
3. Influx 目标端不支持自动创建 schema，会在首次添加数据时自动创建。
4. MySQL 目标端的自动建表为基础建表逻辑，复杂索引、约束和字段类型建议提前手动维护。
5. 事务控制当前仅对 MySQL 作为目标端时有效。

## 开发阶段

因精力有限，目前将项目分为四个阶段开发：

- [x] 第一阶段：MySQL -> InfluxDB 数据迁移，从 MySQL 中获取指定的数据，存储入 InfluxDB 指定 measurement 中
  - [x] 提供 MySQL 至 InfluxDB 的数据映射功能
  - [x] 通过配置文件设置数据库用户名、密码、源库、源表等信息
  - [x] 支持按表/数据库导入，支持批量指定表名
  - [x] 支持排除字段
  - [x] 支持分页参数配置
  - [x] 通过开关控制是否启动时同步加载配置
- [ ] 第二阶段：工具增强
  - [x] 重构架构，增加灵活性、可扩展性
  - [x] 支持目标表重命名
  - [x] 支持按照正则匹配源表名
  - [x] 支持设置偏移量，从指定偏移量开始同步数据
  - [ ] 支持可选字段
  - [x] 新增数据适配器，包括不同类型数据对象的互相转换，重构 Influx 数据源，使用新的数据对象
  - [x] 支持 Influx 时间字段配置 `timeField`
  - [x] 支持 Influx 自定义时间解析格式 `timeFormat`
  - [x] 支持 Influx tag 字段映射 `tagFields`
  - [ ] 更灵活的配置方式，通过 picocli 交互式补充文件中未填的配置项
- [ ] 第三阶段：工具扩展
  - [ ] 实现更多可选的源数据库
  - [ ] 实现更多可选的目标数据库
  - [ ] 进一步增加数据库兼容，兼容更多的数据库类型
  - [ ] 改造为 SDK，完成 API 文档
  - [ ] 设计并实现灵活自定义的导入规则
- [ ] 第四阶段：项目重构
  - [ ] 重构底层实现，增强工具可用性
  - [ ] 数据库增量同步功能，设计同步器架构
  - [ ] 完善任务编排、断点恢复与更细粒度同步能力

## 开源协议

[Apache License 2.0](LICENSE)
