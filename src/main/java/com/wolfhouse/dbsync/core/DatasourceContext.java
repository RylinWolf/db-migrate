package com.wolfhouse.dbsync.core;

import com.wolfhouse.dbsync.core.datasource.strategy.DataSourceStrategy;
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
    private Map<String, DataSourceStrategy.TableInfo> destTableMap;
    /** 上下文就绪 */
    private boolean                                   ready;
}
