package com.wolfhouse.dbsync.core;

import com.wolfhouse.dbsync.core.datasource.DatasourceInitializer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据迁移执行器
 *
 * @author Rylin Wolf
 */
@Component
@RequiredArgsConstructor
@Data
@Slf4j
public class MigrationExecutor {
    /** 数据源上下文 */
    private final DatasourceInitializer context;
}
