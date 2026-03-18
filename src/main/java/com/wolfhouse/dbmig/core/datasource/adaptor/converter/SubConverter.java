package com.wolfhouse.dbmig.core.datasource.adaptor.converter;

import com.wolfhouse.dbmig.properties.MigrateProperty;

/**
 * 转换器接口，用于实际类型的转换逻辑
 *
 * @author Rylin Wolf
 */
public interface SubConverter<S, T> {
    /**
     * 转换方法，将源类型转换为目标类型
     *
     * @param source    源对象
     * @param fieldConf 字段配置
     * @return 转换后的目标对象
     */
    T convert(S source, MigrateProperty.Field fieldConf);

    /**
     * 转换方法，将源类型转换为目标类型，若转换结果为null则返回默认值
     *
     * @param source       源对象
     * @param fieldConf    字段配置
     * @param defaultValue 默认值
     * @return 转换后的目标对象或默认值
     */
    default T convertOrDefault(S source, MigrateProperty.Field fieldConf, T defaultValue) {
        T convert = convert(source, fieldConf);
        return convert != null ? convert : defaultValue;
    }

    /**
     * 获取转换目标类型
     *
     * @return 目标类型
     */
    Class<T> getTargetClazz();

    /**
     * 获取转换源类型
     *
     * @return 源类型
     */
    Class<S> getSourceClazz();
}
