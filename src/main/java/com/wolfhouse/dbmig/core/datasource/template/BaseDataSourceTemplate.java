package com.wolfhouse.dbmig.core.datasource.template;

import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.Condition;
import com.wolfhouse.dbmig.core.datasource.template.page.BasePageIterator;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.MigrateProperty;
import org.jspecify.annotations.NonNull;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Supplier;

/**
 * 数据源模板，规范要处理的数据库的操作。
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
public abstract class BaseDataSourceTemplate<R extends BaseSourceData> implements AutoCloseable {
    /** 要忽略字段 */
    protected final Set<String>              ignore;
    /** 要处理的条件集合 */
    protected final List<Condition<?, ?, ?>> conditions;
    /** 是否忽略空值 */
    protected       boolean                  ignoreNull = false;

    {
        ignore     = new HashSet<>();
        conditions = new ArrayList<>();
    }

    /**
     * 初始化数据源
     *
     * @param prop 数据源连接信息
     */
    public abstract void initDatasource(BaseDbProperty prop);

    /**
     * 初始化条件约束，应当通过条件工厂获取当前类的所有条件并保存至 conditions。
     *
     * @param condition 条件配置
     */
    public abstract void initCondition(MigrateProperty.FieldCondition condition);

    /**
     * 批量插入数据
     *
     * @param tableName 执行操作的目标表名
     * @param data      要插入的数据
     * @return 是否插入成功
     */
    public abstract boolean insertBatch(String tableName, Collection<R> data);

    /**
     * 分页查询数据
     *
     * @param tableName 执行操作的目标表名
     * @param pageSize  每页大小
     * @param pageNum   页码
     * @param offset    偏移量，从第 offset 条数据(不包括)开始查询
     * @return 查询到的数据集合
     */
    public abstract List<R> queryBatch(String tableName, int pageSize, int pageNum, long offset);

    public abstract BasePageIterator<R> page(String tableName, Integer pageSize, long offset);

    /**
     * 获取数据源中的数据总量
     *
     * @param tableName 执行操作的目标表名
     * @return 数据总量
     */
    public abstract long count(String tableName);

    /**
     * 获取当前数据源下的所有表名
     *
     * @return 数据表名集合
     */
    public abstract Set<String> tableNames();

    /**
     * 获取指定表中所有列名
     *
     * @param tableName 数据表名
     * @return 列名集合
     */
    public abstract Set<String> columnNames(String tableName);

    /**
     * 判断当前策略是否支持指定的数据源类型
     *
     * @param prop 数据源连接信息
     * @return 是否支持
     */
    public abstract boolean propertySupport(BaseDbProperty prop);

    /**
     * 判断当前策略是否支持指定的策略类型
     *
     * @param strategy 数据源策略类型
     * @return 是否支持
     */
    public abstract boolean datasourceSupport(BaseDataSourceTemplate<?> strategy);

    /**
     * 获取指定表的表信息封装
     *
     * @param tableName 表名
     * @return 表信息封装
     */
    public abstract TableInfo getTableInfo(String tableName);

    /**
     * 创建表结构
     *
     * @param name 表名
     * @param cols 列集合
     */
    public abstract boolean createSchema(String name, Collection<String> cols);

    /**
     * 全量查询指定表的所有记录
     *
     * @param tableName 表名
     * @param offset    偏移量
     * @return 记录集合
     */
    public abstract Collection<R> queryAll(String tableName, long offset);

    /**
     * 查询是否有指定表
     *
     * @param tableName 表名
     * @return 是否存在
     */
    public abstract boolean hasTable(String tableName);

    /**
     * 配置忽略字段: 在插入、获取数据时忽略这些字段
     *
     * @param ignore 忽略字段
     */
    public final void setIgnore(Collection<String> ignore) {
        this.ignore.addAll(ignore);
    }

    /**
     * 配置是否忽略空值
     *
     * @param ignoreNull 是否忽略空值
     */
    public final void setIgnoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
    }

    /**
     * 检查字段是否被忽略
     *
     * @param col   字段名
     * @param value 字段值
     * @return 是否被忽略
     */
    public final boolean checkIgnore(String col, Object value) {
        return ignore.contains(col) || (ignoreNull && value == null);
    }

    /**
     * 处理输入的数据，移除被忽略的字段。
     *
     * @param data 要处理的数据
     * @return 处理后的数据，其中已移除被忽略的字段
     */
    public BaseSourceData processIgnore(BaseSourceData data) {
        return data.removeIf(entry -> checkIgnore(entry.getKey(), entry.getValue()));
    }

    /**
     * 处理条件约束
     */
    public void processCondition() {
        if (CollectionUtils.isEmpty(conditions)) {
            return;
        }
        this.conditions.forEach(this::doCondition);
    }

    /**
     * 处理单个条件约束
     *
     * @param condition 条件约束
     */
    protected abstract void doCondition(Condition<?, ?, ?> condition);

    /**
     * 获取当前数据源数据对象类型
     *
     * @return 数据对象类型
     */
    public abstract Class<R> getDataClazz();

    /**
     * 在数据源上下文中执行逻辑。默认实现直接执行，特定数据源可重写。
     */
    public <T> T withDataSourceContext(Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * 在数据源上下文中执行无返回值逻辑。
     */
    public void withDataSourceContext(Runnable runnable) {
        withDataSourceContext(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 删除表结构
     *
     * @param tableName 表名
     */
    public abstract void removeSchema(String tableName);

    /**
     * 表信息封装
     *
     * @param name  表名
     * @param alias 表名重命名
     * @param count 总记录行数
     * @param cols  列名
     */
    public record TableInfo(String name, String alias, long count, String... cols) {
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
