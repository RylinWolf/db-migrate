package com.wolfhouse.dbmig.core.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolfhouse.dbmig.core.DatasourceContext;
import com.wolfhouse.dbmig.core.datasource.adaptor.AdaptorFactory;
import com.wolfhouse.dbmig.core.datasource.strategy.condition.ConditionFactory;
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
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据源初始化器
 *
 * @author Rylin Wolf
 */
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
        // 0.先关闭已连接的数据源
        try {
            context.closeAllDatasource();
        } catch (Exception e) {
            log.error("关闭数据源失败", e);
            throw new RuntimeException(e);
        }
        // 1. 校验配置
        propValid(prop);
        // 2. 加载配置
        loadConfig(prop);
        loadDefaultConf(prop);
        // 3. 初始化条件工厂
        ConditionFactory.init(prop.getCore().sourceType());
        // 4. 加载数据源
        loadSource(prop);
        // 5. 根据配置参数，获取目标数据表信息
        loadDestTables(prop);
        // 6. 数据就绪
        context.ready(true);
        // 7. 初始化适配器工厂
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
        context.sourceTemplate(initAndGetDatasource(prop.getDb().source(), prop.getCore().sourceType()));
        log.debug("源数据源已加载: {}", context.sourceTemplate());
        // 2. 加载目标数据源
        context.destTemplate(initAndGetDatasource(prop.getDb().dest(), prop.getCore().destType()));
        log.debug("目标数据源已加载: {}", context.destTemplate());
        // 3. 检查兼容性
        if (!context.sourceTemplate().datasourceSupport(context.destTemplate())) {
            throw new UnsupportedOperationException("不兼容的数据源策略! source: %s, dest: %s".formatted(context.sourceTemplate(), context.destTemplate()));
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

    /** 加载默认值配置 */
    private void loadDefaultConf(MigrateProperty prop) {
        // 加载数据偏移量
        Optional.ofNullable(prop.getCore())
                .flatMap(core -> Optional.ofNullable(core.table()))
                .ifPresent(t -> context.offset(t.offset()));
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
            BaseDataSourceTemplate<?> template = dbType.template.getDeclaredConstructor().newInstance();
            // 初始化数据源
            log.debug("初始化数据源: {}", property);
            template.initDatasource(property);
            // 配置忽略字段
            MigrateProperty.Field field = context.field();
            if (field.ignore() != null) {
                template.setIgnore(Arrays.asList(field.ignore()));
            }
            // 配置忽略空字段
            template.setIgnoreNull(field.ignoreNull());
            // 配置条件字段配置
            template.initCondition(field.condition());
            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDestTables(MigrateProperty prop) {
        // 1. 获取导入模式
        MigrateProperty.Core core = prop.getCore();
        // 获取表配置
        MigrateProperty.TableConf table = core.table();
        // 别名映射
        Map<String, String> aliasMap = table == null || table.aliasMap() == null ? Collections.emptyMap() : table.aliasMap();
        // 导入模式
        MigrateModeEnum mode = core.mode();
        // 目标表集合
        Set<String> tables = new HashSet<>();
        switch (mode) {
            // 按库导入，则获取该库下的所有表
            case DB -> tables.addAll(context.sourceTemplate().tableNames());
            // 按表导入，添加配置的所有表
            // 验证并添加表信息
            case TABLE -> tables.addAll(getDestTables(table));
            default -> throw new IllegalArgumentException("不支持的导入模式: " + mode);
        }
        // 2. 封装表信息对象
        context.targetTableMap(tables.stream()
                                     .map(name -> {
                                         BaseDataSourceTemplate.TableInfo info = context.sourceTemplate().getTableInfo(name);
                                         // 获取表名映射别名，若有别名则获取并构建为新的表信息对象
                                         String alias = aliasMap.get(name);
                                         if (!StringUtils.hasLength(alias)) {
                                             return info;
                                         }
                                         return new BaseDataSourceTemplate.TableInfo(info.name(), alias, info.count(), info.cols());
                                     })
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toMap(BaseDataSourceTemplate.TableInfo::name,
                                                               Function.identity())));
        log.debug("表信息对象加载完毕, 共有 {} 个表: {}", context.targetTableMap().size(), context.targetTableMap().keySet());
    }

    /**
     * 获取所有目标表
     *
     * @param table 表信息
     */
    private Set<String> getDestTables(MigrateProperty.TableConf table) {
        // 1. 校验表信息
        if (table == null) {
            throw new IllegalArgumentException("按表导入模式: 无表配置");
        }
        String[]            tables  = table.tables();
        String[]            pattern = table.pattern();
        Map<String, String> mapping = table.aliasMap();

        boolean noTables  = tables == null || tables.length == 0;
        boolean noPattern = pattern == null || pattern.length == 0;
        boolean noMapping = mapping == null || mapping.isEmpty();

        if (noTables && noPattern && noMapping) {
            throw new IllegalArgumentException("按表导入模式: 未配置目标表");
        }
        // 2. 获取所有目标表
        Set<String> destTables = new HashSet<>();
        // 添加目标表
        if (!noTables) {
            destTables.addAll(Arrays.asList(tables));
        }
        // 若有正则匹配，则获取所有表名
        if (!noPattern) {
            Set<String> existTables = context.sourceTemplate().tableNames();
            existTables.forEach(n -> {
                for (String p : pattern) {
                    if (n.matches(p)) {
                        destTables.add(n);
                    }
                }
            });
        }
        // 若有映射，则获取所有映射键
        if (!noMapping) {
            destTables.addAll(mapping.keySet());
        }
        return destTables;
    }
}
