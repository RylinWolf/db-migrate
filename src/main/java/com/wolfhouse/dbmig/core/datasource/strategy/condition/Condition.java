package com.wolfhouse.dbmig.core.datasource.strategy.condition;

import com.wolfhouse.dbmig.core.datasource.template.BaseDataSourceTemplate;

import java.util.List;

/**
 * 获取字段 条件策略
 *
 * @param <S> 源数据类型
 * @param <T> 目标数据类型
 * @param <P> 执行参数类型
 * @author Rylin Wolf
 */
public interface Condition<S, T, P> {
    /**
     * 执行策略
     *
     * @param source 源数据
     * @param params 执行参数
     * @return 目标数据
     */
    T perform(S source, P params);

    /**
     * 支持的数据源类型
     *
     * @return 数据源类型列表
     */
    List<Class<? extends BaseDataSourceTemplate<?>>> supportTypes();
}
