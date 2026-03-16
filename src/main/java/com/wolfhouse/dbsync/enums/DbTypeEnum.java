package com.wolfhouse.dbsync.enums;

import com.wolfhouse.dbsync.core.datasource.strategy.DataSourceStrategy;
import com.wolfhouse.dbsync.core.datasource.strategy.InfluxSource;
import com.wolfhouse.dbsync.core.datasource.strategy.MySqlSource;
import com.wolfhouse.dbsync.properties.BaseDbProperty;
import com.wolfhouse.dbsync.properties.InfluxProperty;
import com.wolfhouse.dbsync.properties.MySqlProperty;

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

    /** 数据源策略类 */
    public final Class<? extends DataSourceStrategy<?>> strategy;
    /** 数据库配置类 */
    public final Class<? extends BaseDbProperty>        property;

    DbTypeEnum(Class<? extends DataSourceStrategy<?>> strategy,
               Class<? extends BaseDbProperty> property) {
        this.strategy = strategy;
        this.property = property;
    }
}
