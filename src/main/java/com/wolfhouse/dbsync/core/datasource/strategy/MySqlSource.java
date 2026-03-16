package com.wolfhouse.dbsync.core.datasource.strategy;

import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.wolfhouse.dbsync.properties.BaseDbProperty;
import com.wolfhouse.dbsync.properties.MySqlProperty;
import com.zaxxer.hikari.HikariDataSource;
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
public class MySqlSource implements DataSourceStrategy<Map<String, Object>> {
    /** 支持的数据源策略 */
    private static final Set<Class<? extends DataSourceStrategy<?>>> SUPPORTED_STRATEGIES = Set.of(MySqlSource.class,
                                                                                                   InfluxSource.class);

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
        return false;
    }

    @Override
    public List<Map<String, Object>> queryBatch(String tableName, int pageSize, int pageNum) {
        return List.of();
    }

    @Override
    public PageIterator<Map<String, Object>> page(String tableName, Integer pageSize) {
        return null;
    }

    @Override
    public long count(String tableName) {
        return 0;
    }

    @Override
    public Set<String> tableNames() {
        return Set.of();
    }

    @Override
    public boolean propertySupport(BaseDbProperty prop) {
        return prop instanceof MySqlProperty;
    }

    @Override
    public boolean strategySupport(DataSourceStrategy<?> strategy) {
        return SUPPORTED_STRATEGIES.contains(strategy.getClass());
    }

    @Override
    public TableInfo getTableInfo(String tableName) {
        return null;
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
                 .map(Row::toCamelKeysMap)
                 .toList();
    }

    private String buildTable(String tableName, Collection<String> cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `%s` (\n".formatted(tableName))
          .append("  `id` bigint NOT NULL AUTO_INCREMENT,\n");
        cols.forEach(col -> sb.append("  `%s` varchar(255) NOT NULL,\n".formatted(col)));
        sb.append("  PRIMARY KEY (`id`)\n");
        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;");
        return sb.toString();
    }
}
