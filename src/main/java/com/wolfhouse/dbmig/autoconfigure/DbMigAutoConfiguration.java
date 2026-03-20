package com.wolfhouse.dbmig.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolfhouse.dbmig.config.ObjectMapperConfig;
import com.wolfhouse.dbmig.core.DatasourceContext;
import com.wolfhouse.dbmig.core.MigrateExecutor;
import com.wolfhouse.dbmig.core.datasource.DatasourceInitializer;
import com.wolfhouse.dbmig.properties.MigrateProperty;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * dbmig 自动配置
 *
 * @author Rylin Wolf
 */
@AutoConfiguration
@EnableConfigurationProperties(MigrateProperty.class)
@Import(ObjectMapperConfig.class)
public class DbMigAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DatasourceContext datasourceContext() {
        return new DatasourceContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public DatasourceInitializer datasourceInitializer(MigrateProperty migrateProperty,
                                                       ObjectMapper objectMapper,
                                                       DatasourceContext datasourceContext) {
        return new DatasourceInitializer(migrateProperty, objectMapper, datasourceContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrateExecutor migrateExecutor(DatasourceContext datasourceContext) {
        return new MigrateExecutor(datasourceContext);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mig", name = "on-start", havingValue = "true")
    public ApplicationRunner dbMigRunner(DatasourceInitializer datasourceInitializer) {
        return args -> {
            datasourceInitializer.init();
        };
    }
}
