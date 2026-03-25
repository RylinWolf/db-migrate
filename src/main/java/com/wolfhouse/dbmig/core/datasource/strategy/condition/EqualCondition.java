package com.wolfhouse.dbmig.core.datasource.strategy.condition;

/**
 * 等于条件策略
 *
 * @author Rylin Wolf
 */
public interface EqualCondition<S, T, P> extends Condition<S, T, P> {
    /**
     * 执行等于策略
     *
     * @param source 源数据
     * @param params 执行参数
     * @return 目标数据
     */
    default T equal(S source, P params) {
        return perform(source, params);
    }
}
