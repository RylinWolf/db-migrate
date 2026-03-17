package com.wolfhouse.dbsync.core;

import com.mybatisflex.core.row.Db;
import com.wolfhouse.dbsync.core.datasource.strategy.DataSourceStrategy;
import com.wolfhouse.dbsync.core.datasource.strategy.PageIterator;
import com.wolfhouse.dbsync.enums.TransactionGranularityEnum;
import com.wolfhouse.dbsync.properties.SyncProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 数据迁移执行器
 *
 * @author Rylin Wolf
 */
@Component
@RequiredArgsConstructor
@Data
@Slf4j
@SuppressWarnings({"unchecked"})
public class SyncExecutor {
    /** 数据源上下文 */
    private final DatasourceContext context;

    /** 执行同步 */
    public void doSynchronize() {
        log.info("开始数据同步");
        DataSourceStrategy<?>   source   = context.sourceStrategy();
        DataSourceStrategy<?>   dest     = context.destStrategy();
        SyncProperty.Pagination pageConf = context.pagination();

        // 遍历表信息映射，事务级别 任务
        wrapTransaction(TransactionGranularityEnum.TASK, () -> context
                .targetTableMap()
                .values()
                // 事务级别 表
                .forEach(table -> wrapTransaction(
                        TransactionGranularityEnum.TABLE,
                        () -> syncTable(table, dest, pageConf, source))));
        log.info("数据同步结束");
    }

    /**
     * 同步源数据表到目标数据表的方法，根据表记录数和分页配置选择分页或全量同步模式。
     *
     * @param table    表信息封装，包含表名、总记录数以及列名。
     * @param dest     目标数据源策略，用于执行写入操作。
     * @param pageConf 分页配置，包含分页启用状态、每页大小及分页触发阈值。
     * @param source   源数据源策略，用于执行读取操作。
     */
    private void syncTable(DataSourceStrategy.TableInfo table, DataSourceStrategy<?> dest, SyncProperty.Pagination pageConf, DataSourceStrategy<?> source) {
        String tableName = table.name();
        // 表记录不存在，仅构建架构
        if (table.count() <= 0) {
            log.debug("表记录不存在，仅构建架构");
            try {
                dest.createSchema(tableName, Arrays.asList(table.cols()));
            } catch (Exception e) {
                log.error("架构构建失败", e);
            }
            return;
        }
        // 1. 若启用分页，则分页查询
        if (pageConf.enable() && pageConf.critical() >= table.count() && pageConf.size() > 0) {
            log.debug("分页已启用: {}", pageConf);
            PageIterator<?> page = source.page(tableName, pageConf.size());
            while (page.hasNext()) {
                log.debug("表 {} 分页同步数据, size: {}, num: {}, total: {}", tableName, page.pageSize(), page.pageNum(), page.total());
                syncBatch(dest, tableName, page.next());
            }
            return;
        }
        // 2. 未启用分页，全量查询
        log.debug("分页未启用，表 {} 全量同步数据", tableName);
        syncBatch(dest, tableName, source.queryAll(tableName));
    }

    /**
     * 批量写入数据到目标数据源的方法，支持批量插入操作。
     *
     * @param dest      目标数据源策略，用于执行写入操作。
     * @param tableName 表名，用于指定写入的目标表。
     * @param data      数据集合，包含待写入的数据记录。
     * @param <T>       泛型类型参数，表示数据集合中元素的类型。
     */
    private <T> void syncBatch(DataSourceStrategy<T> dest, String tableName, java.util.Collection<?> data) {
        wrapTransaction(TransactionGranularityEnum.PAGE, () -> {
            if (!dest.insertBatch(tableName, (java.util.Collection<T>) data)) {
                log.error("数据写入失败！table: {}, data size: {}", tableName, data.size());
            }
        });
    }

    /**
     * 事务包装器
     *
     * @param runnable 可运行的任务，将在事务中执行。
     */
    private void wrapTransaction(TransactionGranularityEnum currentLevel, Runnable runnable) {
        // 未开启事务 或 当前级别不匹配
        SyncProperty.Transaction config = context.transaction();
        if (!config.enable() || !config.gran().equals(currentLevel)) {
            runnable.run();
            return;
        }
        // 当前级别开启事务
        Db.tx(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("事务执行异常", e);
                return false;
            }
            return true;
        });
    }
}
