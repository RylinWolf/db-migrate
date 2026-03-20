package com.wolfhouse.dbmig.enums;

import com.wolfhouse.dbmig.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbmig.core.datasource.template.InfluxSource;
import com.wolfhouse.dbmig.core.datasource.template.MySqlSource;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.InfluxProperty;
import com.wolfhouse.dbmig.properties.MySqlProperty;

/**
 * 数据库类型枚举类
 *
 * @author Rylin Wolf
 */
public enum DbTypeEnum {
    /** MySQL */
    MYSQL(MySqlSource.class, MySqlProperty.class),
    /** InfluxDB */
    INFLUX(InfluxSource.class, InfluxProperty.class),
    ;

    /** 数据源模板类 */
    public final Class<? extends BaseDataSourceTemplate<?>> template;
    /** 数据库配置类 */
    public final Class<? extends BaseDbProperty>            property;

    DbTypeEnum(Class<? extends BaseDataSourceTemplate<?>> template,
               Class<? extends BaseDbProperty> property) {
        this.template = template;
        this.property = property;
    }
}
