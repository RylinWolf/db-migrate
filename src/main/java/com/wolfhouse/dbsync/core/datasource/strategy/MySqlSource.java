package com.wolfhouse.dbsync.core.datasource.strategy;

import com.wolfhouse.dbsync.properties.BaseDbProperty;
import com.wolfhouse.dbsync.properties.MySqlProperty;

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

    }

    @Override
    public Collection<Map<String, Object>> queryAll(String tableName) {
        return List.of();
    }
}
