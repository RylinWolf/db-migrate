package com.wolfhouse.dbsync.core;

import com.wolfhouse.dbsync.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbsync.properties.MigrateProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据源上下文
 *
 * @author Rylin Wolf
 */
@Component
@Data
@Accessors(fluent = true)
public class DatasourceContext {
    /** 源数据源 */
    private BaseDataSourceTemplate<?>                     sourceStrategy;
    /** 目标数据源 */
    private BaseDataSourceTemplate<?>                     destStrategy;
    /** 目标表映射 */
    private Map<String, BaseDataSourceTemplate.TableInfo> targetTableMap;
    /** 事务配置 */
    private MigrateProperty.Transaction                   transaction;
    /** 分页配置 */
    private MigrateProperty.Pagination                    pagination;
    /** 核心配置 */
    private MigrateProperty.Core                          core;
    /** 字段配置 */
    private MigrateProperty.Field                         field;
    /** 上下文就绪状态 */
    private boolean                                       ready;
}
