package com.wolfhouse.dbsync.core.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolfhouse.dbsync.core.DatasourceContext;
import com.wolfhouse.dbsync.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbsync.enums.DbTypeEnum;
import com.wolfhouse.dbsync.enums.MigrateModeEnum;
import com.wolfhouse.dbsync.properties.BaseDbProperty;
import com.wolfhouse.dbsync.properties.MigrateProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据源初始化器
 *
 * @author Rylin Wolf
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
public class DatasourceInitializer {
    /** 同步器配置 */
    private final MigrateProperty   migrateProperty;
    /** 对象转换器 */
    private final ObjectMapper      objectMapper;
    /** 数据源上下文 */
    private final DatasourceContext context;


    @EventListener(value = ContextRefreshedEvent.class)
    public void init() {
        // 1. 加载数据源
        loadSource();
        // 2. 根据配置参数，获取目标数据表信息
        loadDestTables();
        // 3. 数据就绪
        context.ready(true);
    }

    /**
     * 加载数据源
     */
    private void loadSource() {
        // 1. 加载配置
        loadConfig();
        // 2. 加载源数据源
        context.sourceStrategy(initAndGetDatasource(migrateProperty.getDb().source(), migrateProperty.getCore().sourceType()));
        log.debug("源数据源已加载: {}", context.sourceStrategy());
        // 3. 加载目标数据源
        context.destStrategy(initAndGetDatasource(migrateProperty.getDb().dest(), migrateProperty.getCore().descType()));
        log.debug("目标数据源已加载: {}", context.destStrategy());
        // 4. 检查兼容性
        if (!context.sourceStrategy().strategySupport(context.destStrategy())) {
            throw new UnsupportedOperationException("不兼容的数据源策略! source: %s, dest: %s".formatted(context.sourceStrategy(), context.destStrategy()));
        }
    }

    /** 加载事务、分页、核心配置 */
    private void loadConfig() {
        context.transaction(migrateProperty.getTransaction());
        log.debug("加载事务配置: {}", context.transaction());
        context.pagination(migrateProperty.getPagination());
        log.debug("加载分页配置: {}", context.pagination());
        context.core(migrateProperty.getCore());
        log.debug("加载核心配置: {}", context.core());
        context.field(migrateProperty.getField());
        log.debug("加载字段配置: {}", context.field());
    }

    /**
     * 初始化数据源。根据目标数据库类型获取对应的数据源策略、数据库配置并初始化。
     *
     * @param dbType 数据库类型
     * @return 数据源策略
     */
    private BaseDataSourceTemplate<?> initAndGetDatasource(Map<String, Object> prop, @NonNull DbTypeEnum dbType) {
        try {
            // 获取该数据库的配置类
            BaseDbProperty property = dbType.property.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(objectMapper.convertValue(prop, dbType.property), property);
            // 通过反射构建该数据库的数据源对象
            BaseDataSourceTemplate<?> strategy = dbType.strategy.getDeclaredConstructor(ObjectMapper.class).newInstance(objectMapper);
            // 初始化数据源
            log.debug("初始化数据源: {}", property);
            strategy.initDatasource(property);
            // 配置忽略字段
            MigrateProperty.Field field = context.field();
            if (field.ignore() != null) {
                strategy.setIgnore(Arrays.asList(field.ignore()));
            }
            return strategy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDestTables() {
        // 1. 获取导入模式
        MigrateProperty.Core core   = migrateProperty.getCore();
        MigrateModeEnum      mode   = core.mode();
        Set<String>          tables = new HashSet<>();
        switch (mode) {
            case DB -> {
                // 按库导入，则获取该库下的所有表
                tables.addAll(context.sourceStrategy().tableNames());
            }
            case TABLE -> {
                // 按表导入，添加配置的所有表
                MigrateProperty.TableMode table = core.table();
                if (table == null) {
                    throw new IllegalArgumentException("[sync:core:table] 未配置要导入的表!");
                }
                tables.addAll(Arrays.asList(table.tables()));
            }
            default -> throw new IllegalArgumentException("不支持的导入模式: " + mode);
        }
        // 2. 封装表信息对象
        context.targetTableMap(tables.stream()
                                     .map(name -> context.sourceStrategy().getTableInfo(name))
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toMap(BaseDataSourceTemplate.TableInfo::name,
                                                               Function.identity())));
        log.debug("表信息对象加载完毕: {}", context.targetTableMap().keySet());
    }
}
