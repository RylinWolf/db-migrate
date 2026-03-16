package com.wolfhouse.dbsync.core.datasource.strategy;

import com.wolfhouse.dbsync.properties.BaseDbProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MySQL 数据源
 *
 * @author Rylin Wolf
 */
public class MySqlSource implements DataSourceStrategy<Map<String, Object>> {
    @Override
    public boolean initDatasource(BaseDbProperty prop) {
        return false;
    }

    @Override
    public boolean performMigration(DataSourceStrategy<?> destStrategy) {
        return false;
    }

    @Override
    public boolean insertBatch(String tableName, Collection<Map<String, Object>> data) {
        return false;
    }

    @Override
    public Collection<Map<String, Object>> selectBatch(String tableName, int pageSize, int pageNum) {
        return List.of();
    }

    @Override
    public long count(String tableName) {
        return 0;
    }

    @Override
    public Collection<String> tableNames() {
        return List.of();
    }

    @Override
    public TableInfo getTableInfo(String tableName) {
        return null;
    }

    @Override
    public boolean isSupport(BaseDbProperty prop) {
        return false;
    }
}
