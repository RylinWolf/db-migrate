package com.wolfhouse.dbmig.core.datasource.strategy.condition.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.EqualCondition;
import com.wolfhouse.dbmig.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbmig.core.datasource.template.MySqlSource;

import java.util.List;
import java.util.Map;

/**
 * MySQL 等于条件策略
 *
 * @author Rylin Wolf
 */
public class MysqlEqual implements EqualCondition<QueryWrapper, QueryWrapper, Map<String, Object>> {
    @Override
    public QueryWrapper perform(QueryWrapper source, Map<String, Object> params) {
        params.forEach(source::eq);
        return source;
    }

    @Override
    public List<Class<? extends BaseDataSourceTemplate<?>>> supportTypes() {
        return List.of(MySqlSource.class);
    }
}
