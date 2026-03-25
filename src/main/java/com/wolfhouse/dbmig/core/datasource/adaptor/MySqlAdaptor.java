package com.wolfhouse.dbmig.core.datasource.adaptor;

import com.wolfhouse.dbmig.core.datasource.adaptor.converter.ConverterFactory;
import com.wolfhouse.dbmig.core.datasource.adaptor.converter.SubConverter;
import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.MySqlData;
import com.wolfhouse.dbmig.properties.MigrateProperty;

import java.util.Collection;
import java.util.Set;

/**
 * MySQL 数据适配器
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"unchecked"})
public class MySqlAdaptor extends BaseDataAdaptor<MySqlData> {
    private static final Set<Class<? extends BaseSourceData>> SUPPORTED_FROM;

    static {
        SUPPORTED_FROM = Set.of(InfluxData.class);
    }

    @Override
    public boolean fromSupport(Class<? extends BaseSourceData> clazz) {
        return SUPPORTED_FROM.contains(clazz);
    }

    @Override
    public <T extends BaseSourceData> MySqlData adaptFrom(T obj) {
        SubConverter<T, MySqlData> converter = (SubConverter<T, MySqlData>) ConverterFactory.getConverter(MySqlData.class, obj.getClass());
        return converter.convert(obj, null);
    }

    @Override
    public <T extends BaseSourceData> Collection<MySqlData> adaptFrom(Collection<T> objs) {
        return objs.stream().map(this::adaptFrom).toList();
    }

    @Override
    public Class<MySqlData> getDataClazz() {
        return MySqlData.class;
    }

    @Override
    public void config(MigrateProperty.Field fieldConf) {

    }
}
