package com.wolfhouse.dbmig.core.datasource.adaptor.converter;

import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.MySqlData;
import com.wolfhouse.dbmig.properties.MigrateProperty;

/**
 * InfluxDB 数据至 MySQL 数据转换器
 *
 * @author Rylin Wolf
 */
public class Influx2MySqlConverter implements SubConverter<InfluxData, MySqlData> {
    @Override
    public MySqlData convert(InfluxData source, MigrateProperty.Field fieldConf) {
        return MySqlData.of(source.toMap());
    }

    @Override
    public Class<MySqlData> getTargetClazz() {
        return MySqlData.class;
    }

    @Override
    public Class<InfluxData> getSourceClazz() {
        return InfluxData.class;
    }
}
