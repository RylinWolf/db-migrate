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
 * InfluxDB 数据对象适配器
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"unchecked"})
public class InfluxAdaptor extends BaseDataAdaptor<InfluxData> {
    private static final Set<Class<? extends BaseSourceData>> SUPPORTED_FROM;

    static {
        SUPPORTED_FROM = Set.of(MySqlData.class);
    }

    private MigrateProperty.Field fieldConf;

    @Override
    public boolean fromSupport(Class<? extends BaseSourceData> clazz) {
        return SUPPORTED_FROM.contains(clazz);
    }

    @Override
    public <S extends BaseSourceData> InfluxData adaptFrom(S obj) {
        SubConverter<S, InfluxData> converter = (SubConverter<S, InfluxData>)
                ConverterFactory.getConverter(InfluxData.class, obj.getClass());

        return converter.convert(obj, fieldConf);
    }

    @Override
    public <T extends BaseSourceData> Collection<InfluxData> adaptFrom(Collection<T> objs) {
        return objs.stream().map(this::adaptFrom).toList();
    }

    @Override
    public Class<InfluxData> getDataClazz() {
        return InfluxData.class;
    }

    @Override
    public void config(MigrateProperty.Field fieldConf) {
        this.fieldConf = fieldConf;
    }
}
