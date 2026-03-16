package com.wolfhouse.dbsync.core.datasource.strategy;

import com.influxdb.v3.client.InfluxDBClient;
import com.wolfhouse.dbsync.properties.BaseDbProperty;
import com.wolfhouse.dbsync.properties.InfluxProperty;
import com.wolfhouse.influxclient.client.InfluxClient;
import com.wolfhouse.influxclient.core.InfluxQueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * InfluxDB 数据源
 *
 * @author Rylin Wolf
 */
@Slf4j
public class InfluxSource implements DataSourceStrategy<Map<String, Object>> {
    /** 支持的数据源策略 */
    private static final Set<Class<? extends DataSourceStrategy<?>>> SUPPORTED_STRATEGIES = Set.of(InfluxSource.class,
                                                                                                   MySqlSource.class);
    /** Influx 客户端 */
    private              InfluxClient                                client;

    @Override
    public void initDatasource(BaseDbProperty prop) {
        if (!propertySupport(prop)) {
            throw new IllegalArgumentException("不支持该数据源配置: %s".formatted(prop.getClass()));
        }
        InfluxProperty influxProp = (InfluxProperty) prop;
        client = new InfluxClient(
                InfluxDBClient.getInstance(
                        "http://%s:%s".formatted(influxProp.getHost(), influxProp.getPort()),
                        influxProp.getToken().toCharArray(),
                        influxProp.getDatabase()
                ));
        log.debug("Influx 客户端初始化成功: {}", client.client.getServerVersion());
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
    public Collection<Map<String, Object>> queryAll(String tableName) {
        return client.queryMap(client.addQueryAll(InfluxQueryWrapper.create(tableName)));
    }

    @Override
    public long count(String tableName) {
        return client.count(InfluxQueryWrapper.create(tableName));
    }

    @Override
    public Set<String> tableNames() {
        return Set.copyOf(client.tableNames());
    }

    @Override
    public boolean propertySupport(BaseDbProperty prop) {
        return prop instanceof InfluxProperty;
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
}
