package com.wolfhouse.dbsync;

import com.wolfhouse.dbsync.core.MigrateExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Rylin Wolf
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DbMigApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context  = SpringApplication.run(DbMigApplication.class, args);
        MigrateExecutor                executor = context.getBean(MigrateExecutor.class);
        executor.doSynchronize();
        context.close();
    }
}
