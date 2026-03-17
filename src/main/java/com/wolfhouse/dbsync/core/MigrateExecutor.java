package com.wolfhouse.dbsync.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.wolfhouse.dbsync.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbsync.core.datasource.template.PageIterator;
import com.wolfhouse.dbsync.enums.TransactionGranularityEnum;
import com.wolfhouse.dbsync.properties.MigrateProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 数据迁移执行器
 *
 * @author Rylin Wolf
 */
@Component
@RequiredArgsConstructor
@Data
@Slf4j
public class MigrateExecutor {
    /** 数据源上下文 */
    private final DatasourceContext            context;
    private final DefaultTransactionDefinition defaultTransactionDefinition;
    private final ObjectMapper                 objectMapper;
    private       ThreadPoolTaskExecutor       taskExecutor;
    private       List<Runnable>               failedTasks;
    private       List<CompletableFuture<?>>   tasks = new ArrayList<>();


    @PostConstruct
    public void init() {
        this.failedTasks  = new ArrayList<>();
        this.taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(64);
        taskExecutor.setCorePoolSize(24);
        taskExecutor.setQueueCapacity(1000);
        taskExecutor.setThreadNamePrefix("dbsync-");
        taskExecutor.setRejectedExecutionHandler((r, executor) -> {
            // 任务完成状态
            boolean done = false;
            // 重试次数
            int retry = 10;
            while (retry-- > 0) {
                try {
                    executor.execute(r);
                    done = true;
                    break;
                } catch (Exception ignored) {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            if (!done) {
                failedTasks.add(r);
            }
        });
        taskExecutor.initialize();
    }

    @PreDestroy
    public void destroy() {
        this.taskExecutor.shutdown();
        log.debug("线程池已关闭");
    }

    /** 执行同步 */
    public void doSynchronize() {
        log.info("开始数据同步");
        BaseDataSourceTemplate<?>  source   = context.sourceStrategy();
        BaseDataSourceTemplate<?>  dest     = context.destStrategy();
        MigrateProperty.Pagination pageConf = context.pagination();

        // 遍历表信息映射，事务级别 任务
        wrapTransaction(TransactionGranularityEnum.TASK, () -> context
                .targetTableMap()
                .values()
                // 事务级别 表
                .forEach(table -> wrapTransaction(
                        TransactionGranularityEnum.TABLE,
                        () -> syncTable(table, dest, pageConf, source))));
        // 阻塞等待所有任务完成
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
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
    private void syncTable(BaseDataSourceTemplate.TableInfo table, BaseDataSourceTemplate<?> dest, MigrateProperty.Pagination pageConf, BaseDataSourceTemplate<?> source) {
        String tableName = table.name();

        // 表记录不存在，仅构建架构
        if (table.count() <= 0) {
            log.debug("表 {} 记录不存在，仅构建架构", table.name());
            try {
                dest.createSchema(tableName, Arrays.asList(table.cols()));
            } catch (UnsupportedOperationException ignored) {
                log.debug("架构为构建：数据源 {} 不支持构建架构", dest.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("架构构建失败", e);
            }
            return;
        }
        // 1. 若启用分页，则分页查询
        if (pageConf.enable() && pageConf.critical() <= table.count() && pageConf.size() > 0) {
            log.debug("满足分页条件: count: {}, config: {}", table.count(), pageConf);
            PageIterator<?> page = source.page(tableName, pageConf.size());
            while (page.hasNext()) {
                List<?> next = page.next();
                if (CollectionUtils.isEmpty(next)) {
                    return;
                }
                log.debug("表 {} 分页同步数据, size: {}, num: {}, total: {}", tableName, page.pageSize(), page.pageNum(), page.total());
                tasks.add(CompletableFuture.runAsync(() -> syncBatch(dest, tableName, next), taskExecutor).exceptionally(t -> {
                    log.error("分页同步数据失败，table: {}, size: {}, num: {}", tableName, page.pageSize(), page.pageNum(), t);
                    return null;
                }));
            }
            return;
        }
        // 2. 未启用分页，全量查询
        log.debug("分页未启用，表 {} 全量同步数据", tableName);
        tasks.add(CompletableFuture.runAsync(() -> syncBatch(dest, tableName, source.queryAll(tableName)), taskExecutor).exceptionally(t -> {
            log.error("全量同步数据失败，table: {},", tableName, t);
            return null;
        }));
    }

    /**
     * 批量写入数据到目标数据源的方法，支持批量插入操作。
     *
     * @param dest      目标数据源策略，用于执行写入操作。
     * @param tableName 表名，用于指定写入的目标表。
     * @param data      数据集合，包含待写入的数据记录。
     * @param <T>       泛型类型参数，表示数据集合中元素的类型。
     */
    private <T> void syncBatch(BaseDataSourceTemplate<T> dest, String tableName, java.util.Collection<?> data) {
        wrapTransaction(TransactionGranularityEnum.PAGE, () -> {
            Collection<T> convertedData = objectMapper.convertValue(data, new TypeReference<>() {});
            if (!dest.insertBatch(tableName, convertedData)) {
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
        MigrateProperty.Transaction config = context.transaction();
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
