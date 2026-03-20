package com.wolfhouse.dbmig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author Rylin Wolf
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DbMigApplication {
    public static void main(String[] args) {
        SpringApplication.run(DbMigApplication.class, args);
    }
}
