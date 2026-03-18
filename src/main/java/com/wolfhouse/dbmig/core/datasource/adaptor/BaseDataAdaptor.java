package com.wolfhouse.dbmig.core.datasource.adaptor;

import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.properties.MigrateProperty;

import java.util.Collection;

/**
 * 数据适配器
 *
 * @author Rylin Wolf
 */
public abstract class BaseDataAdaptor<S extends BaseSourceData> {
    /**
     * 是否支持指定类型转换为当前类型
     *
     * @param clazz 指定类型
     * @return 支持与否
     */
    public abstract boolean fromSupport(Class<? extends BaseSourceData> clazz);

    /**
     * 将指定类型转换为当前类型
     *
     * @param obj 指定类型实例
     * @return 当前类型实例
     */
    public abstract <T extends BaseSourceData> S adaptFrom(T obj);

    /**
     * 批量将指定类型集合转换为当前类型集合
     *
     * @param objs 指定类型实例集合
     * @return 当前类型实例集合
     */
    public abstract <T extends BaseSourceData> Collection<S> adaptFrom(Collection<T> objs);

    /**
     * 获取当前适配器支持的数据类型
     *
     * @return 数据类型
     */
    public abstract Class<S> getDataClazz();

    /**
     * 配置适配器
     *
     * @param fieldConf 字段配置
     */
    public abstract void config(MigrateProperty.Field fieldConf);
}
