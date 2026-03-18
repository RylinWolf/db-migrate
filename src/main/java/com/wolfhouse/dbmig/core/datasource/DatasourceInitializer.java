package com.wolfhouse.dbmig.core.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolfhouse.dbmig.core.DatasourceContext;
import com.wolfhouse.dbmig.core.datasource.adaptor.AdaptorFactory;
import com.wolfhouse.dbmig.core.datasource.template.BaseDataSourceTemplate;
import com.wolfhouse.dbmig.enums.DbTypeEnum;
import com.wolfhouse.dbmig.enums.MigrateModeEnum;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.MigrateProperty;
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
    void initOnStart() {
        if (!migrateProperty.isOnStart()) {
            // 未启用启动时加载
            return;
        }
        init();
    }

    /**
     * 初始化配置，加载数据源、目标表等信息
     */
    public void init() {
        initUsing(migrateProperty);
    }

    /**
     * 使用指定配置初始化
     *
     * @param prop 迁移配置
     */
    public void initUsing(MigrateProperty prop) {
        // 0. 校验配置
        propValid(prop);
        // 1. 加载配置
        loadConfig(prop);
        // 2. 加载数据源
        loadSource(prop);
        // 3. 根据配置参数，获取目标数据表信息
        loadDestTables(prop);
        // 4. 数据就绪
        context.ready(true);
        // 5. 初始化适配器工厂
        AdaptorFactory.init(prop.getField());
    }

    /**
     * 验证配置项
     *
     * @param prop 要验证的配置项
     */
    private void propValid(MigrateProperty prop) {
        if (prop.getCore() == null) {
            throw new NullPointerException("缺少核心配置: core");
        }
        if (prop.getDb() == null) {
            throw new NullPointerException("缺少数据库配置: db");
        }
    }

    /**
     * 加载数据源
     */
    private void loadSource(MigrateProperty prop) {
        // 1. 加载源数据源
        context.sourceStrategy(initAndGetDatasource(prop.getDb().source(), prop.getCore().sourceType()));
        log.debug("源数据源已加载: {}", context.sourceStrategy());
        // 2. 加载目标数据源
        context.destStrategy(initAndGetDatasource(prop.getDb().dest(), prop.getCore().destType()));
        log.debug("目标数据源已加载: {}", context.destStrategy());
        // 3. 检查兼容性
        if (!context.sourceStrategy().strategySupport(context.destStrategy())) {
            throw new UnsupportedOperationException("不兼容的数据源策略! source: %s, dest: %s".formatted(context.sourceStrategy(), context.destStrategy()));
        }
    }

    /** 加载事务、分页、核心配置 */
    private void loadConfig(MigrateProperty prop) {
        context.transaction(prop.getTransaction());
        log.debug("加载事务配置: {}", context.transaction());
        context.pagination(prop.getPagination());
        log.debug("加载分页配置: {}", context.pagination());
        context.core(prop.getCore());
        log.debug("加载核心配置: {}", context.core());
        context.field(prop.getField());
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
            // 配置忽略空字段
            strategy.setIgnoreNull(field.ignoreNull());
            return strategy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDestTables(MigrateProperty prop) {
        // 1. 获取导入模式
        MigrateProperty.Core core   = prop.getCore();
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
                if (table == null || table.tables() == null || table.tables().length == 0) {
                    throw new IllegalArgumentException("按表导入模式: 未配置要导入的表!");
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
        log.debug("表信息对象加载完毕, 共有 {} 个表: {}", context.targetTableMap().size(), context.targetTableMap().keySet());
    }
}
