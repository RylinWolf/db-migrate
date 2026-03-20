package com.wolfhouse.dbmig.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * InfluxDB 配置项
 *
 * @author Rylin Wolf
 */
@Data
@ConfigurationProperties(prefix = "db.influx")
@Configuration
@EqualsAndHashCode(callSuper = false)
public class InfluxProperty extends BaseDbProperty {
    private String              host     = "localhost";
    private String              port     = "8181";
    private String              token    = System.getenv("INFLUX_TOKEN");
    private String              database = "dbMig";
    /** 指定数据源时间字段列名 */
    private String              timeField;
    /** 指定数据源时间格式 */
    private String              timeFormat;
    private Map<String, String> tagFields;
}
