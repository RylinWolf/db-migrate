package com.wolfhouse.dbmig.core;

import com.mybatisflex.core.row.Db;
import com.wolfhouse.dbmig.core.datasource.adaptor.AdaptorFactory;
import com.wolfhouse.dbmig.core.datasource.adaptor.BaseDataAdaptor;
import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import com.wolfhouse.dbmig.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbmig.core.datasource.template.page.BasePageIterator;
import com.wolfhouse.dbmig.enums.TransactionGranularityEnum;
import com.wolfhouse.dbmig.properties.MigrateProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 数据迁移执行器
 *
 * @author Rylin Wolf
 */
@RequiredArgsConstructor
@Data
@Slf4j
public class MigrateExecutor {
    /** 数据源上下文 */
    private final DatasourceContext          context;
    private       ThreadPoolTaskExecutor     taskExecutor;
    /** 失败任务列表 */
    private       List<Runnable>             rejectedTasks;
    /** 正在进行的任务列表 */
    private       List<CompletableFuture<?>> tasks = new ArrayList<>();

    @PostConstruct
    void init() {
        this.rejectedTasks = new ArrayList<>();
        this.taskExecutor  = new ThreadPoolTaskExecutor();
        // 配置线程池：任务是 IO 密集型，所以线程池数量可以略高一些
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
                log.error("线程池繁忙，重试次数耗尽仍无法执行任务");
                rejectedTasks.add(r);
            }
        });
        taskExecutor.initialize();
    }

    @PreDestroy
    void destroy() {
        this.taskExecutor.shutdown();
        log.debug("线程池已关闭");
    }

    /** 执行同步 */
    public void doSynchronize() {
        if (!context.ready()) {
            log.error("数据同步失败：上下文未初始化或数据尚未准备完成！");
            return;
        }
        log.info("开始数据同步");
        BaseDataSourceTemplate<?>  source   = context.sourceTemplate();
        BaseDataSourceTemplate<?>  dest     = context.destTemplate();
        MigrateProperty.Pagination pageConf = context.pagination();

        // 本次同步添加的表，若任务执行失败则需要删除
        Set<String> tableRemove = new HashSet<>();
        // DDL 可能触发隐式提交，先在事务外初始化目标表结构
        context.targetTableMap().values().forEach(table -> {
            if (initSchemaSafe(dest, table)) {
                tableRemove.add(table.alias());
            }
        });

        // 遍历表信息映射，事务级别 任务
        try {
            wrapTransaction(TransactionGranularityEnum.TASK,
                            () -> context.targetTableMap()
                                         .values()
                                         // 事务级别 表
                                         .forEach(table -> wrapTransaction(
                                                 TransactionGranularityEnum.TABLE,
                                                 () -> syncTable(source, dest, table, pageConf))));
            // 运行成功，则清空需要删除的表
            tableRemove.clear();
        } catch (Exception e) {
            log.error("数据同步失败", e);
            throw e;
        } finally {
            for (String s : tableRemove) {
                try {
                    dest.removeSchema(s);
                } catch (UnsupportedOperationException ignored) {
                    break;
                } catch (Exception e2) {
                    log.error("删除表失败: {}", s, e2);
                }
            }
        }
        // 阻塞等待所有任务完成
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
                         .join();
        log.info("数据同步结束");
    }

    /** 在事务外初始化目标表结构，避免 DDL 影响事务回滚行为。 */
    private boolean initSchemaSafe(BaseDataSourceTemplate<?> dest, BaseDataSourceTemplate.TableInfo table) {
        String tableName = table.name();
        String alias     = table.alias();
        try {
            return initSchema(dest, table, alias, tableName);
        } catch (UnsupportedOperationException ignored) {
            log.debug("架构为构建：数据源 {} 不支持构建架构", dest.getClass().getSimpleName());
            return false;
        } catch (Exception e) {
            log.error("架构构建失败", e);
            throw e;
        }
    }

    /**
     * 初始化目标表架构
     *
     * @param dest      目标源
     * @param table     表信息
     * @param alias     表别名
     * @param tableName 源表名
     */
    private boolean initSchema(BaseDataSourceTemplate<?> dest, BaseDataSourceTemplate.TableInfo table, String alias, String tableName) {
        // 表名不存在，构建架构
        if (!dest.hasTable(alias)) {
            log.debug("目标表 {} 不存在，创建架构。 源: {}，列: {}", alias, tableName, table.cols());
            List<String> colList = new ArrayList<>(Arrays.asList(table.cols()));
            colList.remove("id");
            dest.createSchema(alias, colList);
            return true;
        }
        return false;
    }

    /**
     * 同步源数据表到目标数据表的方法，根据表记录数和分页配置选择分页或全量同步模式。
     *
     * @param table    表信息封装，包含表名、总记录数以及列名。
     * @param dest     目标数据源策略，用于执行写入操作。
     * @param pageConf 分页配置，包含分页启用状态、每页大小及分页触发阈值。
     * @param source   源数据源策略，用于执行读取操作。
     */
    private <T extends BaseSourceData> void syncTable(
            BaseDataSourceTemplate<T> source,
            BaseDataSourceTemplate<?> dest,
            BaseDataSourceTemplate.TableInfo table,
            MigrateProperty.Pagination pageConf) {
        // 异步开关：未启用事务或事务等级为分页
        boolean async     = !context.transaction().enable() || context.transaction().gran() == TransactionGranularityEnum.PAGE;
        String  tableName = table.name();
        String  alias     = table.alias();

        if (table.count() <= 0) {
            log.debug("表 {} 记录不存在，仅构建架构", table.name());
            return;
        }

        // 1. 若启用分页，则分页查询
        if (pageConf.enable() && pageConf.critical() <= table.count() && pageConf.size() > 0) {
            log.debug("满足分页条件: count: {}, config: {}", table.count(), pageConf);
            BasePageIterator<T> page = source.page(tableName, pageConf.size(), context.offset());
            while (page.hasNext()) {
                List<T> next = page.next();
                if (CollectionUtils.isEmpty(next)) {
                    return;
                }
                log.debug("表 {} 分页同步数据, size: {}, num: {}, total: {}", tableName, page.pageSize(), page.pageNum(), page.total());
                if (!async) {
                    syncBatch(dest, tableName, alias, next);
                    continue;
                }
                CompletableFuture<Void> task = CompletableFuture
                        .runAsync(() -> syncBatch(dest, tableName, alias, next), taskExecutor)
                        .exceptionally(t -> {
                            log.error("分页同步数据失败，table: {}, size: {}, num: {}", tableName, page.pageSize(), page.pageNum(), t);
                            throw new RuntimeException(t);
                        });
                tasks.add(task.whenComplete((v, t) -> tasks.remove(task)));
            }
            return;
        }
        // 2. 未启用分页，全量查询
        log.debug("分页未启用或未触发，表 {} 全量同步数据", tableName);
        if (!async) {
            syncBatch(dest, tableName, alias, source.queryAll(tableName, context.offset()));
            return;
        }
        CompletableFuture<Void> task = CompletableFuture
                .runAsync(() -> syncBatch(dest, tableName, alias, source.queryAll(tableName, context.offset())), taskExecutor)
                .exceptionally(t -> {
                    log.error("全量同步数据失败，table: {},", tableName, t);
                    throw new RuntimeException(t);
                });
        tasks.add(task.whenComplete((v, t) -> tasks.remove(task)));
    }

    /**
     * 批量写入数据到目标数据源的方法，支持批量插入操作。
     *
     * @param dest      目标数据源策略，用于执行写入操作。
     * @param tableName 表名，用于指定写入的目标表。
     * @param data      数据集合，包含待写入的数据记录。
     * @param <T>       泛型类型参数，表示数据集合中元素的类型。
     */
    private <T extends BaseSourceData> void syncBatch(BaseDataSourceTemplate<T> dest,
                                                      String tableName,
                                                      String aliasName,
                                                      Collection<? extends BaseSourceData> data) {
        if (CollectionUtils.isEmpty(data)) {
            // 数据为空，忽略
            return;
        }
        wrapTransaction(TransactionGranularityEnum.PAGE, () -> {
            // 获取适配器
            BaseDataAdaptor<T>              adaptor     = AdaptorFactory.getAdaptor(dest.getDataClazz());
            Class<? extends BaseSourceData> sourceClass = data.stream().iterator().next().getClass();
            if (!adaptor.fromSupport(sourceClass)) {
                throw new IllegalArgumentException(String.format("目标数据源 %s 不支持该数据类型：%s", dest.getDataClazz().getName(), sourceClass.getName()));
            }
            // 使用适配器转换
            Collection<T> convertedData = adaptor.adaptFrom(data);
            if (!dest.insertBatch(aliasName, convertedData)) {
                log.error("数据写入失败！table: {}, alias: {}, data size: {}", tableName, aliasName, data.size());
                throw new RuntimeException(String.format("数据写入失败，已触发回滚。table: %s, alias: %s", tableName, aliasName));
            }
        });
    }

    /**
     * 事务包装器
     *
     * @param runnable 可运行的任务，将在事务中执行。
     */
    private void wrapTransaction(TransactionGranularityEnum currentLevel, Runnable runnable) {
        // 未开启事务
        MigrateProperty.Transaction config = context.transaction();
        if (!config.enable()) {
            runnable.run();
            return;
        }
        // 配置了事务等级，但级别不匹配
        if (currentLevel != null && !config.gran().equals(currentLevel)) {
            runnable.run();
            return;
        }
        // 开启事务，级别匹配或未设置级别
        context.destTemplate().withDataSourceContext(() -> Db.tx(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("事务执行时出现异常，执行回滚。当前事务等级: {}", currentLevel, e);
                throw e;
            }
            return true;
        }));
    }
}
