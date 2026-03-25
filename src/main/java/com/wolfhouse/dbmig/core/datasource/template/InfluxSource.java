package com.wolfhouse.dbmig.core.datasource.template;

import com.influxdb.v3.client.InfluxDBClient;
import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.Condition;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.ConditionFactory;
import com.wolfhouse.dbmig.core.datasource.template.page.InfluxPager;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.InfluxProperty;
import com.wolfhouse.dbmig.properties.MigrateProperty;
import com.wolfhouse.influxclient.client.InfluxClient;
import com.wolfhouse.influxclient.core.InfluxQueryWrapper;
import com.wolfhouse.influxclient.pojo.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * InfluxDB 数据源
 *
 * @author Rylin Wolf
 */
@Slf4j
public class InfluxSource extends BaseDataSourceTemplate<InfluxData> {
    /** Influx 客户端 */
    private InfluxClient client;
    /** 时间字段列名 */
    private String       timeField;
    /** 解析时间格式 */
    private String       timeFormat;
    /** 标签列 */
    private Set<String>  tagFields;
    /** 单次插入数据时，允许的最大字节数 */
    private Long         allowedBufferSize;

    @Override
    public void initDatasource(BaseDbProperty prop) {
        if (!propertySupport(prop)) {
            throw new IllegalArgumentException("不支持该数据源配置: %s".formatted(prop.getClass()));
        }
        InfluxProperty influxProp = (InfluxProperty) prop;
        // 初始化客户端
        client = new InfluxClient(
                InfluxDBClient.getInstance(
                        "http://%s:%s".formatted(influxProp.getHost(), influxProp.getPort()),
                        influxProp.getToken().toCharArray(),
                        influxProp.getDatabase()
                ));
        log.debug("Influx 客户端初始化成功: {}", client.client.getServerVersion());
        // 配置时间字段与标签字段
        timeField  = influxProp.getTimeField();
        timeFormat = influxProp.getTimeFormat();
        Map<String, String> tagFieldsMap = influxProp.getTagFields();
        if (tagFieldsMap != null) {
            tagFields = Set.copyOf(tagFieldsMap.values());
        }
        // 初始化允许的最大字节数
        allowedBufferSize = influxProp.getBufferSize();
        log.debug("Influx 数据源初始化完成");
    }

    @Override
    public void initCondition(MigrateProperty.FieldCondition condition) {
        List<Condition<?, ?, ?>> cList = ConditionFactory.getCondition(InfluxSource.class);
        if (CollectionUtils.isEmpty(cList)) {
            return;
        }
        conditions.addAll(cList);
    }

    @Override
    protected void doCondition(Condition<?, ?, ?> condition) {
    }

    @Override
    public boolean insertBatch(String tableName, Collection<InfluxData> data) {
        try {
            // 遍历 map，构造 Influx 操作对象
            List<AbstractActionInfluxObj> objs = new ArrayList<>();
            data.forEach(m -> objs.add(createObj(m, tableName)));
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
    public List<InfluxData> queryBatch(String tableName, int pageSize, int pageNum, long offset) {
        InfluxPage<InfluxResult> res = client.pagination(InfluxQueryWrapper.create(tableName), InfluxResult.class, pageNum, pageSize, offset);
        return res.records().stream().map(r -> {
            List<Map<String, Object>> map = r.toMap();
            // 移除排除字段
            return map.stream().map(m -> processIgnore(InfluxData.of(m))).toList();
        }).flatMap(List::stream).toList();
    }

    @Override
    public InfluxPager page(String tableName, Integer pageSize, long offset) {
        return InfluxPager.of(pageSize,
                              count(tableName),
                              InfluxQueryWrapper.create().modify().offset(offset).getParent(),
                              tableName,
                              InfluxData::of);
    }

    @Override
    public Collection<InfluxData> queryAll(String tableName, long offset) {
        List<Map<String, Object>> maps = client.queryMap(client.addQueryAll(InfluxQueryWrapper.create(tableName))
                                                               .modify()
                                                               .offset(offset)
                                                               .getParent());
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
    public boolean datasourceSupport(BaseDataSourceTemplate<?> strategy) {
        return DatasourceSupports.INFLUX.contains(strategy.getClass());
    }

    @Override
    public TableInfo getTableInfo(String tableName) {
        return new TableInfo(tableName, tableName, count(tableName), columnNames(tableName).toArray(String[]::new));
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
        // 处理忽略字段
        InfluxData          influxData = (InfluxData) data;
        Map<String, Object> map        = influxData.toMap();
        map.entrySet().removeIf(e -> checkIgnore(e.getKey(), e.getValue()));
        // 若配置了时间字段，则在这里也忽略，插入时特定处理
        if (timeField != null) {
            map.remove(timeField);
        }
        return InfluxData.of(map);
    }

    @Override
    public Class<InfluxData> getDataClazz() {
        return InfluxData.class;
    }

    @Override
    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
            log.debug("Influx 数据源已关闭");
        }
    }

    /**
     * 计算批量插入的大小，根据数据量判断每批次可容纳的数据个
     *
     * @param data 要插入的数据
     * @return 每批次插入数量
     */
    private int calcPageSize(Collection<InfluxData> data) {
        if (CollectionUtils.isEmpty(data) || allowedBufferSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) allowedBufferSize / String.valueOf(data.iterator().next()).length());
    }

    /**
     * 从数据中提取标签与字段
     *
     * @param data 要保存的数据
     * @return 封装的标签字段记录
     */
    private InfluxExtractData extractData(Map<String, Object> data) {
        if (this.tagFields == null) {
            return new InfluxExtractData(Collections.emptyMap(), data);
        }
        Map<String, String> tags   = new HashMap<>(this.tagFields.size());
        Map<String, Object> fields = new HashMap<>(data.size() - this.tagFields.size());

        data.keySet().forEach(k -> {
            if (tagFields.contains(k)) {
                tags.put(k, data.get(k).toString());
                return;
            }
            fields.put(k, data.get(k));
        });
        return new InfluxExtractData(tags, fields);
    }

    /**
     * 根据 InfluxData 构建 Influx 保存数据对象
     *
     * @param data      InfluxData
     * @param tableName 表名
     * @return AbstractActionInfluxObj
     */
    private AbstractActionInfluxObj createObj(InfluxData data, String tableName) {
        return new AbstractActionInfluxObj() {
            {
                // 移除排除字段
                Map<String, Object> ignoredMap  = processIgnore(data).toMap();
                InfluxExtractData   extractData = extractData(ignoredMap);
                // 设置标签
                addTags(InfluxTags.of(extractData.tags));
                // 设置字段
                addFields(InfluxFields.of(extractData.fields));
                // 设置表名
                setMeasurement(tableName);
                // 若传递了时间字段，则设置时间
                Object time = data.getData().get(timeField);
                setTime(solveTimeField(time));
            }
        };
    }

    /**
     * 处理时间字段。若传递了时间，则使用指定时间，否则使用当前时间。
     * <p>
     * 使用指定的时间字段获取时间，若时间字段为字符串，则尝试使用指定的时间格式解析。
     *
     * @param time 时间数据
     * @return Instant 实例
     */
    private Instant solveTimeField(Object time) {
        if (time == null) {
            return Instant.now();
        }
        if (Instant.class.isAssignableFrom(time.getClass())) {
            return (Instant) time;
        }
        // 按照时间格式解析
        return timeFormat == null ?
                Instant.parse(time.toString()) :
                LocalDateTime.parse(time.toString(), DateTimeFormatter.ofPattern(timeFormat))
                             .atZone(ZoneId.systemDefault())
                             .toInstant();
    }

    record InfluxExtractData(Map<String, String> tags, Map<String, Object> fields) {}
}
