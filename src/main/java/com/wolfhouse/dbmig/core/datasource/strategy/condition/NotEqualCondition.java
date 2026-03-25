package com.wolfhouse.dbmig.core.datasource.strategy.condition;

/**
 * 不等于条件策略
 *
 * @author Rylin Wolf
 */
public interface NotEqualCondition<S, T, P> extends Condition<S, T, P> {
    /**
     * 执行不等于策略
     *
     * @param source 源数据
     * @param params 执行参数
     * @return 目标数据
     */
    default T notEqual(S source, P params) {
        return perform(source, params);
    }
}
