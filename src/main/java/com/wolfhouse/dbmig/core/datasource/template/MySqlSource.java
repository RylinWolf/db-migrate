package com.wolfhouse.dbmig.core.datasource.template;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.wolfhouse.dbmig.constant.MigConstant;
import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.MySqlData;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.Condition;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.ConditionFactory;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.impl.MysqlEqual;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.impl.MysqlNotEqual;
import com.wolfhouse.dbmig.core.datasource.template.page.MysqlPager;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.MigrateProperty;
import com.wolfhouse.dbmig.properties.MySqlProperty;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * MySQL 数据源。
 * 注意，该数据源线程不安全！
 * 在并发环境中应当使用多例创建。
 *
 * @author Rylin Wolf
 */
@Slf4j
@Accessors(fluent = true)
public class MySqlSource extends BaseDataSourceTemplate<MySqlData> {
    @Setter
    @Getter
    private boolean                        ignoreNull        = false;
    /** 基础查询构造器 */
    private QueryWrapper                   baseQueryWrapper;
    /** 基础查询构造器使用标识 */
    private boolean                        isWrapperConsumed = false;
    /** 字段条件配置 */
    private MigrateProperty.FieldCondition fieldCondition;
    /** 当前使用的数据源 */
    private HikariDataSource               currentDs;

    @Override
    public void initDatasource(BaseDbProperty prop) {
        if (!propertySupport(prop)) {
            throw new IllegalArgumentException("不支持该数据源配置: %s".formatted(prop.getClass()));
        }
        // 若当前使用的数据源未关闭，则先关闭
        close();
        MySqlProperty mysqlProp = (MySqlProperty) prop;
        // 根据配置创建数据源
        HikariDataSource ds = new HikariDataSource();
        ds.setUsername(mysqlProp.getUsername());
        ds.setPassword(mysqlProp.getPassword());
        ds.setJdbcUrl("jdbc:mysql://%s:%s/%s".formatted(mysqlProp.getHost(), mysqlProp.getPort(), mysqlProp.getDatabase()));
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // 配置当前数据源
        currentDs = ds;
        // 兼容非 Spring Boot 环境
        if (initFlexBootstrap(ds)) {
            return;
        }
        FlexGlobalConfig globalConfig  = FlexGlobalConfig.getDefaultConfig();
        Configuration    configuration = globalConfig.getConfiguration();
        if (configuration == null) {
            throw new RuntimeException("FlexGlobalConfig 配置意外为空");
        }
        globalConfig.getDataSource().addDataSource(MigConstant.DATASOURCE_KEY, ds);
    }

    @Override
    public void initCondition(MigrateProperty.FieldCondition condition) {
        List<Condition<?, ?, ?>> cList = ConditionFactory.getCondition(MySqlSource.class);
        if (CollectionUtils.isEmpty(cList)) {
            return;
        }
        conditions.addAll(cList);
        fieldCondition = condition;
        // 初始化条件构造器
        processCondition();
    }

    @Override
    public void processCondition() {
        // 初始化查询构造器
        baseQueryWrapper = QueryWrapper.create();
        super.processCondition();
    }

    @Override
    protected void doCondition(Condition<?, ?, ?> c) {
        if (fieldCondition == null) {
            return;
        }
        // MySQL 字段值等于
        if (c instanceof MysqlEqual equal) {
            Map<String, Object> equalMap = fieldCondition.equal();
            if (CollectionUtils.isEmpty(equalMap)) {
                return;
            }
            equal.perform(baseQueryWrapper, equalMap);
            return;
        }
        // MySQL 字段值不等于
        if (c instanceof MysqlNotEqual notEqual) {
            Map<String, Object> notEqualMap = fieldCondition.notEqual();
            if (CollectionUtils.isEmpty(notEqualMap)) {
                return;
            }
            notEqual.perform(baseQueryWrapper, notEqualMap);
            return;
        }
        log.error("MySql 数据源未配置的条件类型：{}", c.getClass().getSimpleName());
    }

    @Override
    public boolean insertBatch(String tableName, Collection<MySqlData> data) {
        try {
            List<Row> rowList = data.stream().map(m -> {
                Row row = new Row();
                // 排除忽略字段
                row.putAll(processIgnore(m).toMap());
                return row;
            }).toList();
            DataSourceKey.use(MigConstant.DATASOURCE_KEY, () -> {
                Db.insertBatch(tableName, rowList);
            });
            return true;
        } catch (Exception e) {
            log.error("批量添加数据失败！", e);
            return false;
        }
    }

    @Override
    public List<MySqlData> queryBatch(String tableName, int pageSize, int pageNum, long offset) {
        return dataSourceKeySurround(() -> Db.paginate(tableName, pageNum, pageSize, queryWrapper().offset(offset))
                                             .getRecords()
                                             .stream()
                                             .map(r -> processIgnore(MySqlData.of(r.toUnderlineKeysMap())))
                                             .toList());
    }

    @Override
    public MysqlPager<MySqlData> page(String tableName, Integer pageSize, long offset) {
        return MysqlPager.of(pageSize, count(tableName), queryWrapper().offset(offset), tableName, MySqlData::of);
    }

    @Override
    public long count(String tableName) {
        return dataSourceKeySurround(() -> Db.selectCountByQuery(tableName, queryWrapper()));
    }

    @Override
    public Set<String> tableNames() {
        // 执行原生的 SHOW TABLES 语句
        return Set.copyOf(dataSourceKeySurround(
                () -> Db.selectListBySql("SHOW TABLES")
                        .stream()
                        .map(map -> map.values()
                                       .iterator()
                                       .next()
                                       .toString())
                        .toList()));
    }

    @Override
    public Set<String> columnNames(String tableName) {
        List<Row> rows = dataSourceKeySurround(() -> Db.selectListBySql("SHOW COLUMNS FROM `%s`".formatted(tableName)));
        if (CollectionUtils.isEmpty(rows)) {
            return Set.of();
        }
        return Set.copyOf(rows.stream()
                              .map(r -> String.valueOf(r.toUnderlineKeysMap().get("field")))
                              .toList());
    }

    @Override
    public boolean propertySupport(BaseDbProperty prop) {
        return prop instanceof MySqlProperty;
    }

    @Override
    public boolean datasourceSupport(BaseDataSourceTemplate<?> strategy) {
        return DatasourceSupports.MYSQL.contains(strategy.getClass());
    }

    @Override
    public TableInfo getTableInfo(String tableName) {
        return new TableInfo(tableName, tableName, count(tableName), columnNames(tableName).toArray(String[]::new));
    }

    @Override
    public boolean hasTable(String tableName) {
        return tableNames().contains(tableName);
    }

    @Override
    public void createSchema(String name, Collection<String> cols) {
        if (!StringUtils.hasLength(name) || CollectionUtils.isEmpty(cols)) {
            return;
        }
        DataSourceKey.use(MigConstant.DATASOURCE_KEY, () -> Db.updateBySql(buildTable(name, cols)));
    }

    @Override
    public Collection<MySqlData> queryAll(String tableName, long offset) {
        return dataSourceKeySurround(() -> Db.selectListByQuery(tableName, queryWrapper())
                                             .stream()
                                             .skip(offset)
                                             // 移除排除字段
                                             .map(r -> processIgnore(MySqlData.of(r.toUnderlineKeysMap())))
                                             .toList());
    }

    @Override
    public MySqlData processIgnore(BaseSourceData data) {
        MySqlData           mysqlData = (MySqlData) data;
        Map<String, Object> map       = mysqlData.toMap();
        map.entrySet().removeIf(e -> checkIgnore(e.getKey(), e.getValue()));
        return MySqlData.of(map);
    }

    @Override
    public Class<MySqlData> getDataClazz() {
        return MySqlData.class;
    }

    @Override
    public void close() {
        // 移除数据源
        FlexGlobalConfig config = FlexGlobalConfig.getDefaultConfig();
        if (config != null && config.getConfiguration() != null) {
            config.getDataSource().removeDatasource(MigConstant.DATASOURCE_KEY);
        }
        if (currentDs != null && !currentDs.isClosed()) {
            currentDs.close();
            log.debug("MySql 数据源已关闭");
        }
        currentDs = null;
    }

    private String buildTable(String tableName, Collection<String> cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `%s` (\n".formatted(tableName))
          .append("  `id` bigint NOT NULL AUTO_INCREMENT,\n");
        cols.forEach(col -> sb.append("  `%s` varchar(255) NULL,\n".formatted(col)));
        sb.append("  PRIMARY KEY (`id`)\n");
        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;");
        return sb.toString();
    }

    private <T> T dataSourceKeySurround(Supplier<T> supplier) {
        try {
            DataSourceKey.use(MigConstant.DATASOURCE_KEY);
            return supplier.get();
        } finally {
            DataSourceKey.clear();
        }
    }

    /**
     * 内部私有方法：处理 MyBatis-Flex 引擎的首次启动
     * 采用双重检查锁（DCL）确保线程安全
     *
     * @return 是否进行了初始化
     */
    private boolean initFlexBootstrap(HikariDataSource ds) {
        if (FlexGlobalConfig.getDefaultConfig().getConfiguration() == null) {
            synchronized (MybatisFlexBootstrap.class) {
                if (FlexGlobalConfig.getDefaultConfig().getConfiguration() == null) {
                    log.debug("MyBatis-Flex 引擎未启动，正在执行首次初始化...");
                    MybatisFlexBootstrap.getInstance()
                                        .setLogImpl(Slf4jImpl.class)
                                        .addDataSource(MigConstant.DATASOURCE_KEY, ds)
                                        .start();
                    return true;
                    // 此时 ds 已经通过 start() 注册进去了，不需要额外 add
                }
            }
        }
        return false;
    }

    private QueryWrapper queryWrapper() {
        // 若未使用过，则直接返回
        if (!isWrapperConsumed) {
            isWrapperConsumed = true;
            return baseQueryWrapper;
        }

        // 使用过，更新并置否
        // 处理条件，在其中初始化了 queryWrapper
        processCondition();
        isWrapperConsumed = false;
        return baseQueryWrapper;
    }
}
