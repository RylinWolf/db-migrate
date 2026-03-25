package com.wolfhouse.dbmig.core.datasource.strategy.condition;

import com.wolfhouse.dbmig.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbmig.enums.DbTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 字段条件工厂
 *
 * @author Rylin Wolf
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public final class ConditionFactory {
    /** 是否初始化完成 */
    public static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /** 条件映射，数据源 - 条件列表 */
    private static final Map<Class<? extends BaseDataSourceTemplate<?>>, List<Condition<?, ?, ?>>> CONDITION_MAP;

    static {
        CONDITION_MAP = new ConcurrentHashMap<>();
    }

    private ConditionFactory() {
    }

    /**
     * 初始化条件工厂
     *
     * @param sourceType 源数据源类型
     */
    public static void init(DbTypeEnum sourceType) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        log.debug("初始化条件工厂，源数据源类型: {}", sourceType);
        // 数据源类型
        Class<? extends BaseDataSourceTemplate<?>> sourceTempClz = sourceType.template;
        // 条件类数量
        int conditionSize = 0;
        // 获取所有条件类，遍历
        ServiceLoader<Condition> conditionLoader = ServiceLoader.load(Condition.class);
        for (Condition condition : conditionLoader) {
            // 构建支持类 - 条件 映射
            for (Class type : (List<Class>) condition.supportTypes()) {
                // 只保留数据源匹配的值
                if (sourceTempClz.isAssignableFrom(type)) {
                    CONDITION_MAP.computeIfAbsent(type, k -> new ArrayList<>())
                                 .add(condition);
                    conditionSize++;
                }
            }
        }
        log.debug("条件工厂初始化完成，共 {} 个目标类型，{} 个条件", CONDITION_MAP.size(), conditionSize);
    }

    /**
     * 获取指定数据源类型的所有条件
     *
     * @param sourceTempClz 数据源类型
     * @return 条件列表
     */
    public static List<Condition<?, ?, ?>> getCondition(Class<? extends BaseDataSourceTemplate<?>> sourceTempClz) {
        return CONDITION_MAP.get(sourceTempClz);
    }
}
