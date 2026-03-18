package com.wolfhouse.dbmig.core.datasource.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.v3.client.InfluxDBClient;
import com.mybatisflex.core.query.QueryWrapper;
import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.InfluxProperty;
import com.wolfhouse.influxclient.client.InfluxClient;
import com.wolfhouse.influxclient.core.InfluxQueryWrapper;
import com.wolfhouse.influxclient.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * InfluxDB 数据源
 *
 * @author Rylin Wolf
 */
@Slf4j
public class InfluxSource extends BaseDataSourceTemplate<InfluxData> {
    private final ObjectMapper objectMapper;
    /** Influx 客户端 */
    private       InfluxClient client;

    public InfluxSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
    public boolean insertBatch(String tableName, Collection<InfluxData> data) {
        try {
            // 遍历 map，构造 Influx 操作对象
            List<AbstractActionInfluxObj> objs = new ArrayList<>();
            data.forEach(m -> objs.add(new AbstractActionInfluxObj() {{
                // 移除排除字段
                Map<String, Object> ignoredMap = processIgnore(m).toMap();
                // 设置字段
                addFields(InfluxFields.of(ignoredMap));
                // 初始化标签
                this.tags = InfluxTags.instance();
                // 设置表名
                setMeasurement(tableName);
            }}));
            int batchSize = calcPageSize(data);
            log.debug("表 {} 计算每批次数量: {}，执行分批插入", tableName, batchSize);
            client.insertBatch(objs, batchSize);
            return true;
        } catch (Exception e) {
            log.error("表 {} 插入数据失败", tableName, e);
            return false;
        }
    }

    @Override
    public List<InfluxData> queryBatch(String tableName, int pageSize, int pageNum) {
        InfluxPage<InfluxResult> res = client.pagination(InfluxQueryWrapper.create(tableName), InfluxResult.class, pageNum, pageSize);
        return res.records().stream().map(r -> {
            List<Map<String, Object>> map = r.toMap();
            // 移除排除字段
            return map.stream().map(m -> processIgnore(InfluxData.of(m))).toList();
        }).flatMap(List::stream).toList();
    }

    @Override
    public PageIterator<InfluxData> page(String tableName, Integer pageSize) {
        return PageIterator.of(pageSize, count(tableName), QueryWrapper.create(), objectMapper, tableName, InfluxData::of);
    }

    @Override
    public Collection<InfluxData> queryAll(String tableName) {
        List<Map<String, Object>> maps = client.queryMap(client.addQueryAll(InfluxQueryWrapper.create(tableName)));
        return maps.stream().map(m -> processIgnore(InfluxData.of(m)))
                   .toList();
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
    public Set<String> columnNames(String tableName) {
        return Set.copyOf(client.tableColumns(tableName));
    }

    @Override
    public boolean propertySupport(BaseDbProperty prop) {
        return prop instanceof InfluxProperty;
    }

    @Override
    public boolean strategySupport(BaseDataSourceTemplate<?> strategy) {
        return StrategySupports.INFLUX.contains(strategy.getClass());
    }

    @Override
    public TableInfo getTableInfo(String tableName) {
        return new TableInfo(tableName, count(tableName), columnNames(tableName).toArray(String[]::new));
    }

    @Override
    public boolean hasTable(String tableName) {
        return client.tableNames().contains(tableName);
    }

    @Override
    public void createSchema(String name, Collection<String> cols) {
        throw new UnsupportedOperationException("Influx 数据源不支持创建表架构");
    }

    @Override
    public InfluxData processIgnore(BaseSourceData data) {
        InfluxData          influxData = (InfluxData) data;
        Map<String, Object> map        = influxData.toMap();
        map.entrySet().removeIf(e -> checkIgnore(e.getKey(), e.getValue()));
        return InfluxData.of(map);
    }

    /**
     * 计算批量插入的大小，根据数据量判断每批次可容纳的数据个
     *
     * @param data 要插入的数据
     * @return 每批次插入数量
     */
    private int calcPageSize(Collection<InfluxData> data) {
        if (CollectionUtils.isEmpty(data)) {
            return 0;
        }
        return (int) Math.ceil((double) 10_000 / String.valueOf(data.iterator().next()).length());
    }

    @Override
    public Class<InfluxData> getDataClazz() {
        return InfluxData.class;
    }
}
