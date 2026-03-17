package com.wolfhouse.dbsync.properties;

import com.wolfhouse.dbsync.enums.DbTypeEnum;
import com.wolfhouse.dbsync.enums.MigrateModeEnum;
import com.wolfhouse.dbsync.enums.TransactionGranularityEnum;
import lombok.Data;
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
public class MigrateProperty {
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
            if (enable && gran == null) {
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
            if (enable && size == null) {
                size = 200;
            }
            if (enable && critical == null) {
                critical = 300;
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
     * @param descType   目标数据库类型
     */
    public record Core(MigrateModeEnum mode,
                       TableMode table,
                       DbTypeEnum sourceType,
                       DbTypeEnum descType) {}

    /**
     * 字段配置
     *
     * @param ignore 忽略字段
     */
    public record Field(String[] ignore, Boolean ignoreNull) {
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
    }
}
