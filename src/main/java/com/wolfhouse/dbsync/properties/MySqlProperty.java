package com.wolfhouse.dbsync.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MySQL 数据库配置
 *
 * @author Rylin Wolf
 */
@Configuration
@ConfigurationProperties(prefix = "db.mysql")
@Data
@EqualsAndHashCode(callSuper = false)
public class MySqlProperty extends BaseDbProperty {
    private String host     = "localhost";
    private String port     = "3306";
    private String database;
    private String table;
    private String username = "root";
    private String password;
}
