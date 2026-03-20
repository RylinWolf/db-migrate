package com.wolfhouse.dbmig.core;

import com.wolfhouse.dbmig.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbmig.properties.MigrateProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 数据源上下文
 *
 * @author Rylin Wolf
 */
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

    // region 默认状态属性
    /** 查找数据偏移量，跳过 offset 条数据 */
    private long offset;
    // endregion
}
