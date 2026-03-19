# DB Migrate (db-mig)

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-23-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk23-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)

`db-migrate` 是一个轻量级的通用数据库同步工具，基于 Spring Boot 3.5 开发。旨在实现异构数据源（目前支持 MySQL 和
InfluxDB）之间的高效、灵活的数据迁移与同步。

## 🚀 核心特性

- **多源支持**：目前支持 MySQL 到 InfluxDB 的数据同步。通过策略模式、模板方法模式和 SPI，提供良好的扩展性。
- **高性能并行同步**：
    - 内置线程池，支持多表、多批次并行处理。
    - 智能分页机制，针对海量数据自动触发分页查询，降低内存压力。
- **灵活的同步模式**：
    - **全库同步**：自动发现源库所有表并同步。
    - **指定表同步**：精确控制需要同步的表清单。
- **完善的数据处理**：
    - **架构自动构建**：支持在目标库不存在表时自动尝试构建基本 Schema。
    - **字段过滤**：支持全局配置忽略特定字段。
    - **空值处理**：支持配置是否忽略 `null` 值写入。
- **事务与可靠性**：
    - 提供事务粒度控制（TASK/PAGE），确保数据一致性（目前主要针对 MySQL 目标源）。
    - 详细的同步日志与异常处理机制。

## 🛠️ 环境要求

- **Java**: 23+ (使用最新的 Java 特性如 Record)
- **Maven**: 3.9+
- **数据库**: MySQL 8.0+, InfluxDB (通过 influxclient4j 支持)

## 📦 快速开始

### 1. 克隆并编译

```bash
git clone https://github.com/your-repo/db-migrate.git
cd db-migrate
mvn clean install
```

### 2. 配置同步任务

新建 `src/main/resources/migConf.yml` 文件，配置源端和目标端连接信息及同步策略。

```yaml
mig:
  on-start: true      # 是否在应用启动时立即执行同步

  core:
    mode: db          # 同步模式: db (全库), table (指定表)
    source-type: mysql # 源数据库类型: mysql, influx
    dest-type: influx  # 目标数据库类型: mysql, influx
    # table:          # 若 mode 为 table，需配置此处
    #   tables: [table1, table2]

  db:
    source:           # 源连接配置
      host: localhost
      database: source_db
      username: root
      password: your_password
    dest:             # 目标连接配置
      host: localhost
      port: 8181
      database: dest_db

  pagination:
    enable: true      # 开启分页
    size: 1000        # 分页大小
    critical: 1000    # 触发分页的阈值（记录数超过此值则分页）

  field:
    ignore:           # 需忽略的通用字段
      - create_time
      - update_time
    ignore-null: false # 是否忽略 null 值
```

### 3. 运行应用

```bash
mvn spring-boot:run
```

同步完成后，应用会自动关闭。

## 📖 详细配置说明

| 配置项                       | 说明               | 默认值 / 可选值                 |
|:--------------------------|:-----------------|:--------------------------|
| `mig.core.mode`           | 同步范围模式           | `db` (全库), `table` (指定表)  |
| `mig.core.source-type`    | 源端类型             | `mysql`, `influx`         |
| `mig.core.dest-type`      | 目标端类型            | `mysql`, `influx`         |
| `mig.pagination.enable`   | 是否启用分页查询         | `true`                    |
| `mig.pagination.size`     | 每页拉取的数据量         | `200`                     |
| `mig.pagination.critical` | 启动分页的最小记录数       | `200`                     |
| `mig.transaction.enable`  | 是否开启事务控制         | `false`                   |
| `mig.transaction.gran`    | 事务粒度             | `PAGE` (页级), `TASK` (任务级) |
| `mig.field.ignore`        | 字符串数组，配置不参与同步的列名 | -                         |

## 🏗️ 架构设计

项目采用高度解耦的策略模式与适配器模式：

- **Template 层**：`BaseDataSourceTemplate` 定义了不同数据库的抽象操作逻辑。
- **Adaptor 层**：`BaseDataAdaptor` 处理跨数据库类型的数据结构转换。
- **Converter 层**：具体的字段级数据转换逻辑。
- **Executor 层**：`MigrateExecutor` 负责整体同步任务的编排、多线程调度与生命周期管理。

## ⚠️ 注意事项

1. **Java 版本**：本项目使用了 Java 23 的新特性，请确保运行环境 JDK 版本正确。
2. **表结构同步**：目前工具支持基础的表结构创建，但对于复杂的索引、外键、约束等，建议预先在目标库手动创建好 Schema 以获得最佳兼容性。
3. **InfluxDB 支持**：同步到 InfluxDB 时，请确保目标数据库（Bucket/Organization）已正确配置。

## 📄 开源协议

[Apache License 2.0](LICENSE)



## 开发阶段

因精力有限，目前将项目分为四个阶段开发：

- [x] 第一阶段：MySQL -> InfluxDB 数据迁移，从 MySQL 中获取指定的数据，存储入 InfluxDB 指定 measurement 中
  - 提供 MySQL 至 InfluxDB 的数据映射功能
  - 通过配置文件设置数据库用户名、密码、源库、源表等信息
  - 支持按表/数据库导入，支持批量指定表名
  - 支持排除字段
  - 支持分页参数配置
  - 通过开关控制是否启动时同步加载配置
- [ ] 第二阶段：工具增强
  - [x] 重构架构，增加灵活性、可扩展性
  - [ ] 支持可选字段
  - [x] 新增数据适配器，包括不同类型数据对象的互相转换，重构 Influx 数据源，使用新的数据对象
  - [ ] 更灵活的配置方式，通过 picocli 交互式补充文件中未填的配置项
- [ ] 第三阶段：工具扩展
  - [ ] 实现更多可选的源数据库
  - [ ] 实现更多可选的目标数据库
  - [ ] 进一步增加数据库兼容，兼容更多的数据库类型
  - [ ] 改造为 SDK，完成 API 文档
  - [ ] 设计并实现灵活自定义的导入规则
- [ ] 第四阶段：项目重构
  - [ ] 重构底层实现，增强工具可用性
  - [ ] 新增数据库同步功能，设计同步器架构，实现 MySQL -> Influx 数据同步
