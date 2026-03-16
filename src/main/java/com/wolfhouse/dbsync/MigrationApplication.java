package com.wolfhouse.dbsync;

import com.wolfhouse.dbsync.core.MigrationExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Rylin Wolf
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MigrationApplication {
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context  = SpringApplication.run(MigrationApplication.class, args);
        MigrationExecutor              executor = context.getBean(MigrationExecutor.class);
    }
}
