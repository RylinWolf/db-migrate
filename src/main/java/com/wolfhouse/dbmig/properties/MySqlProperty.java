package com.wolfhouse.dbmig.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * MySQL 数据库配置
 *
 * @author Rylin Wolf
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class MySqlProperty extends BaseDbProperty {
    private String host     = "localhost";
    private String port     = "3306";
    private String database;
    private String table;
    private String username = "root";
    private String password;
}
