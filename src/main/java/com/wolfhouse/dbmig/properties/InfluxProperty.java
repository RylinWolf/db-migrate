package com.wolfhouse.dbmig.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * InfluxDB 配置项
 *
 * @author Rylin Wolf
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class InfluxProperty extends BaseDbProperty {
    private String              host       = "localhost";
    private String              port       = "8181";
    private String              token      = System.getenv("INFLUX_TOKEN");
    private String              database   = "dbMig";
    /** 单次插入数据时，允许的最大字节数 */
    private Long                bufferSize = 10_000L;
    /** 指定数据源时间字段列名 */
    private String              timeField;
    /** 指定数据源时间格式 */
    private String              timeFormat;
    private Map<String, String> tagFields;
}
