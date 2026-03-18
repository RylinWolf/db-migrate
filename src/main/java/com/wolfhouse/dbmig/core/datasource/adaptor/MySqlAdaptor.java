package com.wolfhouse.dbmig.core.datasource.adaptor;

import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.sourcedata.MySqlData;
import com.wolfhouse.dbmig.properties.MigrateProperty;

import java.util.Collection;

/**
 * MySQL 数据适配器
 *
 * @author Rylin Wolf
 */
public class MySqlAdaptor extends BaseDataAdaptor<MySqlData> {
    @Override
    public boolean fromSupport(Class<? extends BaseSourceData> clazz) {
        return false;
    }

    @Override
    public <T extends BaseSourceData> MySqlData adaptFrom(T obj) {
        return null;
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
