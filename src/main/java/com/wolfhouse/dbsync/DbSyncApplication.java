package com.wolfhouse.dbsync;

import com.wolfhouse.dbsync.core.SyncExecutor;
import com.wolfhouse.dbsync.core.datasource.strategy.DataSourceStrategy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Collection;

/**
 * @author Rylin Wolf
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DbSyncApplication {
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context  = SpringApplication.run(DbSyncApplication.class, args);
        SyncExecutor                   executor = context.getBean(SyncExecutor.class);
        DataSourceStrategy<?>          source   = executor.getContext().sourceStrategy();
        DataSourceStrategy<?>          dest     = executor.getContext().destStrategy();

        executor.doSynchronize();
        //if (!source.hasTable("testTable")) {
        //    source.createSchema("testTable", Set.of("name"))
        //}
        //System.out.println(source.getTableInfo("testTable"))
        //System.out.println(dest.getTableInfo("testTable"));
        // region 插入

        //Map<String, Object> dataMap = new HashMap<>(1);
        //dataMap.put("name", "rylin");
        //insert(source, List.of(dataMap, dataMap, dataMap))
        //insert(dest, List.of(dataMap, dataMap, dataMap));

        // endregion

        // region 分页

        //PageIterator<?> iterator = source.page("testTable", 2)
        //while (iterator.hasNext()) {
        //    System.out.println(iterator.next())
        //}

        //PageIterator<?> iterator = dest.page("testTable", 2)
        //while (iterator.hasNext()) {
        //    System.out.println(iterator.next())
        //}
        // endregion

    }

    @SuppressWarnings("unchecked")
    public static <T> void insert(DataSourceStrategy<?> source, Collection<T> data) {
        DataSourceStrategy<T> tSource = (DataSourceStrategy<T>) source;
        if (tSource.insertBatch("testTable", data)) {
            System.out.println("插入成功");
            return;
        }
        System.err.println("插入失败");
    }


}
