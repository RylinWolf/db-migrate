package com.wolfhouse.dbsync.core.datasource.strategy;

import java.util.Set;

/**
 * 数据源支持情况，封装了各数据源兼容的其他数据源
 *
 * @author Rylin Wolf
 */
public final class StrategySupports {
    /** MySQL 数据源支持的源 */
    public static final Set<Class<? extends DataSourceStrategy<?>>> MYSQL = Set.of(MySqlSource.class,
                                                                                   InfluxSource.class);

    public static final Set<Class<? extends DataSourceStrategy<?>>> INFLUX = Set.of(InfluxSource.class,
                                                                                    MySqlSource.class);

    private StrategySupports() {
    }
}
