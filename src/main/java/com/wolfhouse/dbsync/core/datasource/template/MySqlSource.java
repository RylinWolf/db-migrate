package com.wolfhouse.dbsync.core.datasource.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.wolfhouse.dbsync.properties.BaseDbProperty;
import com.wolfhouse.dbsync.properties.MySqlProperty;
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

/**
 * MySQL 数据源
 *
 * @author Rylin Wolf
 */
@Slf4j
@Accessors(fluent = true)
public class MySqlSource extends BaseDataSourceTemplate<Map<String, Object>> {
    private final ObjectMapper objectMapper;
    @Setter
    @Getter
    private       boolean      ignoreNull = false;

    public MySqlSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void initDatasource(BaseDbProperty prop) {
        if (!propertySupport(prop)) {
            throw new IllegalArgumentException("不支持该数据源配置: %s".formatted(prop.getClass()));
        }
        MySqlProperty    mysqlProp = (MySqlProperty) prop;
        HikariDataSource ds        = new HikariDataSource();
        ds.setUsername(mysqlProp.getUsername());
        ds.setPassword(mysqlProp.getPassword());
        ds.setJdbcUrl("jdbc:mysql://%s:%s/%s".formatted(mysqlProp.getHost(), mysqlProp.getPort(), mysqlProp.getDatabase()));
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        MybatisFlexBootstrap.getInstance()
                            .setDataSource(ds)
                            .setLogImpl(Slf4jImpl.class)
                            .start();
    }

    @Override
    public boolean insertBatch(String tableName, Collection<Map<String, Object>> data) {
        try {
            List<Row> rowList = data.stream().map(m -> {
                Row row = new Row();
                // 排除忽略字段
                processIgnore(m, true);
                row.putAll(m);
                return row;
            }).toList();
            Db.insertBatch(tableName, rowList);
            return true;
        } catch (Exception e) {
            log.error("批量添加数据失败！", e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> queryBatch(String tableName, int pageSize, int pageNum) {
        return Db.paginate(tableName, pageNum, pageSize, QueryWrapper.create())
                 .getRecords()
                 .stream()
                 .map(r -> {
                     // 排除忽略字段
                     Map<String, Object> map = r.toCamelKeysMap();
                     processIgnore(map, true);
                     return map;
                 })
                 .toList();
    }

    @Override
    public PageIterator<Map<String, Object>> page(String tableName, Integer pageSize) {
        return PageIterator.of(pageSize, count(tableName), QueryWrapper.create(), objectMapper, tableName);
    }

    @Override
    public long count(String tableName) {
        return Db.selectCountByQuery(tableName, QueryWrapper.create());
    }

    @Override
    public Set<String> tableNames() {
        // 执行原生的 SHOW TABLES 语句
        List<String> tables = Db.selectListBySql("SHOW TABLES")
                                .stream()
                                .map(map -> map.values().iterator().next().toString())
                                .toList();

        return Set.copyOf(tables);
    }

    @Override
    public Set<String> columnNames(String tableName) {
        List<Row> rows = Db.selectListBySql("SHOW COLUMNS FROM `%s`".formatted(tableName));
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
        return new TableInfo(tableName, count(tableName), columnNames(tableName).toArray(String[]::new));
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
        Db.updateBySql(buildTable(name, cols));
    }

    @Override
    public Collection<Map<String, Object>> queryAll(String tableName) {
        return Db.selectAll(tableName)
                 .stream()
                 .map(r -> {
                     // 移除排除字段
                     Map<String, Object> map = r.toCamelKeysMap();
                     processIgnore(map, true);
                     return map;
                 })
                 .toList();
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
}
