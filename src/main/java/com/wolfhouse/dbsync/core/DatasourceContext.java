package com.wolfhouse.dbsync.core;

import com.wolfhouse.dbsync.core.datasource.strategy.DataSourceStrategy;
import com.wolfhouse.dbsync.properties.SyncProperty;
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
    private DataSourceStrategy<?>                     sourceStrategy;
    /** 目标数据源 */
    private DataSourceStrategy<?>                     destStrategy;
    /** 目标表映射 */
    private Map<String, DataSourceStrategy.TableInfo> targetTableMap;
    /** 事务配置 */
    private SyncProperty.Transaction                  transaction;
    /** 分页配置 */
    private SyncProperty.Pagination                   pagination;
    /** 核心配置 */
    private SyncProperty.Core                         core;
    /** 上下文就绪状态 */
    private boolean                                   ready;
}
