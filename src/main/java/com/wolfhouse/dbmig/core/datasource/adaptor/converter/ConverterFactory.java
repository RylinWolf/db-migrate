package com.wolfhouse.dbmig.core.datasource.adaptor.converter;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 转换器工厂
 *
 * @author Rylin Wolf
 */
@Slf4j
@SuppressWarnings({"unchecked"})
public class ConverterFactory {
    /** 转换器映射，目标类 - Map(源类 - 转换器) */
    private static final Map<Class<?>, Map<Class<?>, SubConverter<?, ?>>> CONVERTER_MAP;

    static {
        CONVERTER_MAP = new ConcurrentHashMap<>();
        ServiceLoader.load(SubConverter.class).forEach(converter -> {
            Class<?> sourceType = converter.getSourceClazz();
            Class<?> targetType = converter.getTargetClazz();
            CONVERTER_MAP.computeIfAbsent(targetType, k -> new ConcurrentHashMap<>()).put(sourceType, converter);
        });
        log.debug("转换器工厂初始化完成，共 {} 个转换标的", CONVERTER_MAP.size());
    }

    public static <T, S> SubConverter<T, S> getConverter(Class<T> targetType, Class<S> sourceType) {
        Map<Class<?>, SubConverter<?, ?>> typeMap = CONVERTER_MAP.get(targetType);
        if (typeMap == null) {
            throw new IllegalArgumentException("未找到目标类型为 %s 的转换器".formatted(targetType.getName()));
        }
        SubConverter<?, ?> converter = typeMap.get(sourceType);
        if (converter == null) {
            throw new IllegalArgumentException("未找到目标类型为 %s, 源类型为 %s 的转换器".formatted(targetType.getName(), sourceType.getName()));
        }
        return (SubConverter<T, S>) converter;
    }
}
