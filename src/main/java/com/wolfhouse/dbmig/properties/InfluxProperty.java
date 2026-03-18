package com.wolfhouse.dbmig.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    private String host  = "localhost";
    private String port  = "8181";
    private String token = System.getenv("INFLUX_TOKEN");
    private String database;
}
