package com.wolfhouse.dbmig.core.datasource.adaptor.converter;

import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.MySqlData;
import com.wolfhouse.dbmig.properties.MigrateProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        map.replaceAll((k, v) -> {
            // 特殊处理时间字段
            Object instant = processInstant(v);
            return instant != null ? instant : v;
        });
        return InfluxData.of(map);
    }

    /**
     * 处理时间字段。
     * 字段若为时间，则尝试将其封装为 Instant，否则返回 null。
     * 无法封装为 Instant，则返回字符串表示。
     *
     * @param v 要处理的数据
     * @return 若数据可封装为 Instant，则返回为 Instant。若数据为时间，则返回字符串，否则返回 null。
     */
    private Object processInstant(Object v) {
        Class<?> vClass = v.getClass();
        if (!Temporal.class.isAssignableFrom(vClass)) {
            return null;
        }
        if (Instant.class.isAssignableFrom(vClass)) {
            return Instant.from((Temporal) v).toString();
        }
        if (LocalDateTime.class.isAssignableFrom(vClass)) {
            LocalDateTime vDateTime = (LocalDateTime) v;
            return vDateTime.atZone(ZoneOffset.UTC).toInstant().toString();
        }
        return v.toString();
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
