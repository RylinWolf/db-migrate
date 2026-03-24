package com.wolfhouse.dbmig.core.datasource.adaptor.converter;

import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.MySqlData;
import com.wolfhouse.dbmig.properties.MigrateProperty;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Map;

/**
 * MySQL 数据至 InfluxDB 数据转换器
 *
 * @author Rylin Wolf
 */
public class MySql2InfluxConverter implements SubConverter<MySqlData, InfluxData> {
    @Override
    public InfluxData convert(MySqlData source, MigrateProperty.Field fieldConf) {
        Map<String, Object> map = source.toMap();
        map.forEach((k, v) -> {
            // 特殊处理时间字段
            if (Temporal.class.isAssignableFrom(v.getClass())) {
                map.put(k, Instant.from((Temporal) v));
            }
        });
        return InfluxData.of(map);
    }

    @Override
    public Class<InfluxData> getTargetClazz() {
        return InfluxData.class;
    }

    @Override
    public Class<MySqlData> getSourceClazz() {
        return MySqlData.class;
    }
}
