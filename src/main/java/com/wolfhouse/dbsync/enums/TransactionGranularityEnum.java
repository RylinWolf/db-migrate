package com.wolfhouse.dbsync.enums;

/**
 * 事务粒度枚举类
 *
 * @author Rylin Wolf
 */
public enum TransactionGranularityEnum {
    /** 分页粒度，每个分页为一个事务 */
    PAGE,
    /** 表粒度，每张表为一个事务 */
    TABLE,
    /** 任务粒度，每次任务为一个事务。任务只会成功或完全失败。 */
    TASK
}
