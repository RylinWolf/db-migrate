package com.wolfhouse.dbmig.core.datasource.template.page;

import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.influxclient.core.InfluxQueryWrapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

// TODO

/**
 * InfluxDB 分页器
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"rawtypes"})
public class InfluxPager extends BasePageIterator<InfluxData> {
    private final InfluxQueryWrapper                        queryWrapper;
    private final String                                    tableName;
    private final Function<Map<String, Object>, InfluxData> dataMapper;

    private InfluxPager(int pageSize, long total, InfluxQueryWrapper queryWrapper, String tableName, Function<Map<String, Object>, InfluxData> mapper) {
        super(pageSize, total);
        this.queryWrapper = queryWrapper;
        this.tableName    = tableName;
        this.dataMapper   = mapper;
    }

    public static InfluxPager of(int pageSize,
                                 long total,
                                 InfluxQueryWrapper queryWrapper,
                                 String tableName,
                                 Function<Map<String, Object>, InfluxData> mapper) {
        return new InfluxPager(pageSize, total, queryWrapper, tableName, mapper);
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public List<InfluxData> next() {
        return List.of();
    }
}
