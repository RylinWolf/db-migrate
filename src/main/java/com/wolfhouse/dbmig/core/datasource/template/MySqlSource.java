package com.wolfhouse.dbmig.core.datasource.template;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.wolfhouse.dbmig.constant.MigConstant;
import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.MySqlData;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.MySqlProperty;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * MySQL 数据源
 *
 * @author Rylin Wolf
 */
@Slf4j
@Accessors(fluent = true)
public class MySqlSource extends BaseDataSourceTemplate<MySqlData> {
    @Setter
    @Getter
    private boolean ignoreNull = false;

    @Override
    public void initDatasource(BaseDbProperty prop) {
        if (!propertySupport(prop)) {
            throw new IllegalArgumentException("不支持该数据源配置: %s".formatted(prop.getClass()));
        }
        MySqlProperty mysqlProp = (MySqlProperty) prop;
        // 根据配置创建数据源
        HikariDataSource ds = new HikariDataSource();
        ds.setUsername(mysqlProp.getUsername());
        ds.setPassword(mysqlProp.getPassword());
        ds.setJdbcUrl("jdbc:mysql://%s:%s/%s".formatted(mysqlProp.getHost(), mysqlProp.getPort(), mysqlProp.getDatabase()));
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // 兼容 Spring Boot 环境
        FlexGlobalConfig globalConfig  = FlexGlobalConfig.getDefaultConfig();
        Configuration    configuration = globalConfig.getConfiguration();
        if (configuration != null) {
            globalConfig.getDataSource().addDataSource(MigConstant.DATASOURCE_KEY, ds);
            return;
        }
        // 回落保底
        MybatisFlexBootstrap.getInstance()
                            .addDataSource(MigConstant.DATASOURCE_KEY, ds)
                            .setLogImpl(Slf4jImpl.class)
                            .start();
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
        return dataSourceKeySurround(() -> Db.paginate(tableName, pageNum, pageSize, QueryWrapper.create().offset(offset))
                                             .getRecords()
                                             .stream()
                                             .map(r -> processIgnore(MySqlData.of(r.toCamelKeysMap())))
                                             .toList());
    }

    @Override
    public PageIterator<MySqlData> page(String tableName, Integer pageSize, long offset) {
        return PageIterator.of(pageSize, count(tableName), QueryWrapper.create().offset(offset), tableName, MySqlData::of);
    }

    @Override
    public long count(String tableName) {
        return dataSourceKeySurround(() -> Db.selectCountByQuery(tableName, QueryWrapper.create()));
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
                              .map(r -> String.valueOf(r.toCamelKeysMap().get("field")))
                              .toList());
    }

    @Override
    public boolean propertySupport(BaseDbProperty prop) {
        return prop instanceof MySqlProperty;
    }

    @Override
    public boolean strategySupport(BaseDataSourceTemplate<?> strategy) {
        return StrategySupports.MYSQL.contains(strategy.getClass());
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
        return dataSourceKeySurround(() -> Db.selectAll(tableName)
                                             .stream()
                                             .skip(offset)
                                             // 移除排除字段
                                             .map(r -> processIgnore(MySqlData.of(r.toCamelKeysMap())))
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
}
