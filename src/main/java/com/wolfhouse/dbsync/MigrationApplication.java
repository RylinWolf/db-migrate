package com.wolfhouse.dbsync;

import com.wolfhouse.dbsync.core.SyncExecutor;
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
        SyncExecutor                   executor = context.getBean(SyncExecutor.class);
    }
}
