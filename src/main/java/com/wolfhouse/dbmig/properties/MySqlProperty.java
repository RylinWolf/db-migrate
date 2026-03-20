package com.wolfhouse.dbmig.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MySQL 数据库配置
 *
 * @author Rylin Wolf
 */
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
