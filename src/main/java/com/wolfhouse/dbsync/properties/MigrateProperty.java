package com.wolfhouse.dbsync.properties;

import com.wolfhouse.dbsync.enums.DbTypeEnum;
import com.wolfhouse.dbsync.enums.MigrateModeEnum;
import com.wolfhouse.dbsync.enums.TransactionGranularityEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;

/**
 * 同步器配置项
 *
 * @author Rylin Wolf
 */
@ConfigurationProperties(prefix = "mig")
@Configuration
@Data
@Slf4j
public class MigrateProperty {
    /** 启动时加载配置 */
    private boolean     onStart;
    /** 事务配置 */
    private Transaction transaction;
    /** 分页配置 */
    private Pagination  pagination;
    /** 核心配置 */
    private Core        core;
    /** 数据库配置 */
    private Db          db;
    /** 字段配置 */
    private Field       field;

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
                       TableMode table,
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
            // 按表迁移，但未配置表信息
            if (mode == MigrateModeEnum.TABLE) {
                if (table == null || table.tables() == null || table.tables().length == 0) {
                    throw new NullPointerException("缺少必须的配置：core.table");
                }
            }
        }
    }

    /**
     * 字段配置
     *
     * @param ignore 忽略字段
     */
    public record Field(String[] ignore, boolean ignoreNull) {
        @Override
        @NonNull
        public String toString() {
            return "Field{" +
                    "ignore=" + Arrays.toString(ignore) +
                    ", ignoreNull=" + ignoreNull +
                    '}';
        }
    }

    public record TableMode(String... tables) {
    }

    public record Db(Map<String, Object> source, Map<String, Object> dest) {
        public Db {
            if (source == null || dest == null) {
                throw new NullPointerException("缺少必须的配置：db.source、db.dest");
            }
        }
    }
}
