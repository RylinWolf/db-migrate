package com.wolfhouse.dbmig.core.datasource.sourcedata;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 基础数据库数据类型
 *
 * @author Rylin Wolf
 */
public abstract class BaseSourceData {

    /**
     * 将数据源数据转换为 Map 对象
     *
     * @return 包含所有键值对的 Map 对象
     */
    public abstract Map<String, Object> toMap();

    /**
     * 从 Map 对象构建数据源数据
     *
     * @param map 包含键值对的 Map 对象
     * @return 当前 BaseSourceData 实例
     */
    public abstract BaseSourceData fromMap(Map<String, Object> map);

    /**
     * 判断是否包含指定的键
     *
     * @param key 要检查的键名
     * @return 如果包含该键则返回 true，否则返回 false
     */
    public abstract boolean has(String key);

    /**
     * 判断数据源数据是否为空
     *
     * @return 如果数据为空则返回 true，否则返回 false
     */
    public abstract boolean isEmpty();

    /**
     * 获取数据源数据中键值对的数量
     *
     * @return 键值对的数量
     */
    public abstract int size();

    /**
     * 获取数据源数据中所有的键
     *
     * @return 包含所有键的集合
     */
    public abstract Collection<String> keys();

    /**
     * 获取数据源数据中所有的值
     *
     * @return 包含所有值的集合
     */
    public abstract Collection<Object> values();

    /**
     * 移除指定键的键值对
     *
     * @param key 要移除的键名
     * @return 当前 BaseSourceData 实例
     */
    public abstract BaseSourceData remove(String key);

    /**
     * 根据条件移除键值对
     *
     * @param filter 过滤条件，满足条件的键值对将被移除
     * @return 当前 BaseSourceData 实例
     */
    public abstract BaseSourceData removeIf(Predicate<Map.Entry<String, Object>> filter);
}
