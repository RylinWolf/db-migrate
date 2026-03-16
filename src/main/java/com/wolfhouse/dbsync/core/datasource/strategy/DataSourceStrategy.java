package com.wolfhouse.dbsync.core.datasource.strategy;

import com.wolfhouse.dbsync.properties.BaseDbProperty;

import java.util.Collection;

/**
 * 数据源策略，规范要处理的数据库的操作
 *
 * @author Rylin Wolf
 */
public interface DataSourceStrategy<R> {
    /**
     * 初始化数据源
     *
     * @param prop 数据源连接信息
     * @return 是否初始化成功
     */
    boolean initDatasource(BaseDbProperty prop);

    /**
     * 执行数据迁移
     *
     * @param destStrategy 目标数据源策略
     * @return 是否迁移成功
     */
    boolean performMigration(DataSourceStrategy<?> destStrategy);

    /**
     * 批量插入数据
     *
     * @param tableName 执行操作的目标表名
     * @param data      要插入的数据
     * @return 是否插入成功
     */
    boolean insertBatch(String tableName, Collection<R> data);

    /**
     * 分页查询数据
     *
     * @param tableName 执行操作的目标表名
     * @param pageSize  每页大小
     * @param pageNum   页码
     * @return 查询到的数据集合
     */
    Collection<R> selectBatch(String tableName, int pageSize, int pageNum);

    /**
     * 获取数据源中的数据总量
     *
     * @param tableName 执行操作的目标表名
     * @return 数据总量
     */
    long count(String tableName);

    /**
     * 获取当前数据源下的所有表名
     *
     * @return 数据表名集合
     */
    Collection<String> tableNames();

    /**
     * 判断当前策略是否支持指定的数据源类型
     *
     * @param prop 数据源连接信息
     * @return 是否支持
     */
    boolean isSupport(BaseDbProperty prop);

    /**
     * 获取指定表的表信息封装
     *
     * @param tableName 表名
     * @return 表信息封装
     */
    TableInfo getTableInfo(String tableName);


    /**
     * 表信息封装
     *
     * @param name  表名
     * @param count 总记录行数
     * @param cols  列名
     */
    record TableInfo(String name, long count, String... cols) {}
}
