package com.wolfhouse.dbmig.properties;

import com.wolfhouse.dbmig.enums.DbTypeEnum;
import com.wolfhouse.dbmig.enums.MigrateModeEnum;
import com.wolfhouse.dbmig.enums.TransactionGranularityEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 同步器配置项
 *
 * @author Rylin Wolf
 */
@ConfigurationProperties(prefix = "mig")
@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class MigrateProperty {
    /** 启动时加载配置 */
    private boolean     onStart     = false;
    /** 事务配置 */
    private Transaction transaction = new Transaction(false, null);
    /** 分页配置 */
    private Pagination  pagination  = new Pagination(true, 200, 200);
    /** 核心配置 */
    private Core        core;
    /** 数据库配置 */
    private Db          db;
    /** 字段配置 */
    private Field       field       = new Field(null, false, null);

    /**
     * 事务配置记录。注意：事务仅支持 MySQL 数据源作为目标源时才会生效
     *
     * @param enable 是否开启
     * @param gran   事务粒度
     */
    public record Transaction(boolean enable, TransactionGranularityEnum gran) {
        public Transaction(boolean enable, TransactionGranularityEnum gran) {
            this.enable = enable;
            if (!enable) {
                this.gran = null;
                return;
            }
            if (gran == null) {
                log.warn("未配置事务粒度，使用默认值: PAGE");
                gran = TransactionGranularityEnum.PAGE;
            }
            this.gran = gran;
        }
    }

    /**
     * 分页配置记录
     *
     * @param enable   是否开启
     * @param size     每页大小
     * @param critical 启动分页触发阈值，仅当数据总数超过次值时开启分页
     */
    public record Pagination(boolean enable, Integer size, Integer critical) {
        public Pagination(boolean enable, Integer size, Integer critical) {
            this.enable = enable;
            if (!enable) {
                // 未启用分页，初始化参数为 null
                this.size     = null;
                this.critical = null;
                return;
            }
            if (size == null) {
                log.warn("未配置分页大小，使用默认值: 200");
                size = 200;
            }
            if (critical == null) {
                log.warn("未配置启用分页触发阈值，使用默认值: 200");
                critical = 200;
            }
            this.size     = size;
            this.critical = critical;
        }
    }

    /**
     * 核心配置
     *
     * @param mode       同步模式
     * @param table      表模式配置
     * @param sourceType 源数据库类型
     * @param destType   目标数据库类型
     */
    public record Core(MigrateModeEnum mode,
                       TableConf table,
                       DbTypeEnum sourceType,
                       DbTypeEnum destType) {
        public Core {
            // 无数据源类型
            if (sourceType == null || destType == null) {
                throw new NullPointerException("缺少必须的配置：core.source-type、core.dest-type");
            }
            // 未设置模式，默认为 按库迁移
            if (mode == null) {
                log.warn("未设置迁移模式，使用默认值: DB");
                mode = MigrateModeEnum.DB;
            }
            // 按表迁移，但未配置表信息的情况，在获取目标表时进行
        }
    }

    /**
     * 字段配置
     *
     * @param ignore     忽略字段
     * @param ignoreNull 是否忽略空值
     *
     */
    public record Field(String[] ignore, boolean ignoreNull, FieldCondition condition) {

    }

    /**
     * 数据表配置
     *
     * @param pattern  匹配表名，正则表达式
     * @param tables   指定表名
     * @param aliasMap 别名映射，格式 [源表名:目标表名]
     * @param offset   偏移量，从第 offset 条数据开始迁移
     */
    public record TableConf(String[] pattern, String[] tables, Map<String, String> aliasMap, long offset) {
    }

    public record Db(Map<String, Object> source, Map<String, Object> dest) {
        public Db {
            if (source == null || dest == null) {
                throw new NullPointerException("缺少必须的配置：db.source、db.dest");
            }
        }
    }

    /**
     * 字段条件，仅匹配满足条件的字段
     *
     * @param equal    相同值匹配，约束指定字段的值为特定值
     * @param notEqual 不同值匹配，约束指定字段的值不为特定值
     */
    public record FieldCondition(Map<String, Object> equal, Map<String, Object> notEqual) {}
}
