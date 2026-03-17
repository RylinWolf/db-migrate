package com.wolfhouse.dbsync.core.datasource.strategy;

import com.wolfhouse.dbsync.properties.BaseDbProperty;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 数据源策略，规范要处理的数据库的操作
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
public interface DataSourceStrategy<R> {
    /**
     * 初始化数据源
     *
     * @param prop 数据源连接信息
     */
    void initDatasource(BaseDbProperty prop);

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
    List<R> queryBatch(String tableName, int pageSize, int pageNum);

    PageIterator<R> page(String tableName, Integer pageSize);

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
    Set<String> tableNames();

    /**
     * 获取指定表中所有列名
     *
     * @param tableName 数据表名
     * @return 列名集合
     */
    Set<String> columnNames(String tableName);

    /**
     * 判断当前策略是否支持指定的数据源类型
     *
     * @param prop 数据源连接信息
     * @return 是否支持
     */
    boolean propertySupport(BaseDbProperty prop);

    /**
     * 判断当前策略是否支持指定的策略类型
     *
     * @param strategy 数据源策略类型
     * @return 是否支持
     */
    boolean strategySupport(DataSourceStrategy<?> strategy);

    /**
     * 获取指定表的表信息封装
     *
     * @param tableName 表名
     * @return 表信息封装
     */
    TableInfo getTableInfo(String tableName);

    /**
     * 创建表结构
     *
     * @param name 表名
     * @param cols 列集合
     */
    void createSchema(String name, Collection<String> cols);

    /**
     * 全量查询指定表的所有记录
     *
     * @param tableName 表名
     * @return 记录集合
     */
    Collection<R> queryAll(String tableName);

    /**
     * 查询是否有指定表
     *
     * @param tableName 表名
     * @return 是否存在
     */
    boolean hasTable(String tableName);

    /**
     * 配置忽略字段: 在插入、获取数据时忽略这些字段
     *
     * @param ignore 忽略字段
     */
    void setIgnore(Collection<String> ignore);

    /**
     * 表信息封装
     *
     * @param name  表名
     * @param count 总记录行数
     * @param cols  列名
     */
    record TableInfo(String name, long count, String... cols) {
        @Override
        @NonNull
        public String toString() {
            return "TableInfo{" +
                    "name='" + name + '\'' +
                    ", count=" + count +
                    ", cols=" + Arrays.toString(cols) +
                    '}';
        }
    }
}
