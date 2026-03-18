package com.wolfhouse.dbmig.core.datasource.adaptor;

import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.properties.MigrateProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据源适配器工厂
 *
 * @author Rylin Wolf
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public final class AdaptorFactory {
    /** 适配器映射 */
    private static final Map<Class<? extends BaseSourceData>, BaseDataAdaptor<? extends BaseSourceData>> ADAPTOR_MAP;

    /** 是否初始化完成 */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    static {
        ADAPTOR_MAP = new ConcurrentHashMap<>();
    }

    /**
     * 初始化适配器工厂。加载并初始化适配器
     *
     */
    public static void init(MigrateProperty.Field fieldConf) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        ServiceLoader<BaseDataAdaptor> loader = ServiceLoader.load(BaseDataAdaptor.class);
        log.debug("适配器加载完成，共 {} 个", loader.stream().count());
        for (BaseDataAdaptor adaptor : loader) {
            adaptor.config(fieldConf);
            ADAPTOR_MAP.put(adaptor.getDataClazz(), adaptor);
        }
    }

    /**
     * 根据指定的类型，获取该类的适配器
     *
     * @param clazz 指定类型
     * @param <T>   数据类型
     * @return 该类适配器
     */
    public static <T extends BaseSourceData> BaseDataAdaptor<T> getAdaptor(Class<T> clazz) {
        if (!INITIALIZED.get()) {
            throw new IllegalStateException("适配器工厂未初始化!");
        }
        if (!ADAPTOR_MAP.containsKey(clazz)) {
            throw new IllegalArgumentException("适配器未找到，无法适配数据类型：" + clazz.getName());
        }
        return (BaseDataAdaptor<T>) ADAPTOR_MAP.get(clazz);
    }
}
