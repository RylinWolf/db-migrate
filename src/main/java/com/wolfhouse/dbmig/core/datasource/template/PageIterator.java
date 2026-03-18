package com.wolfhouse.dbmig.core.datasource.template;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 分页迭代器
 * <p>
 * 用于分页查询数据库表数据，支持迭代访问每一页的数据。
 * 通过 MyBatis-Flex 的分页查询功能实现数据分页加载，
 * 并使用 ObjectMapper 将查询结果转换为指定的目标类型。
 * </p>
 *
 * @param <T> 数据类型
 * @author Rylin Wolf
 */
@Accessors(fluent = true)
public class PageIterator<T extends BaseSourceData> implements Iterator<List<T>> {
    /**
     * 每页数据量
     */
    @Getter
    private final int          pageSize;
    /**
     * 数据总数
     */
    @Getter
    private final long         total;
    /**
     * 查询条件包装器
     */
    private final QueryWrapper wrapper;
    /**
     * 表名
     */
    private final String       tableName;
    /**
     * 当前页码，从 0 开始
     */
    @Getter
    private final AtomicLong   pageNum = new AtomicLong(0);

    private final Function<Map<String, Object>, T> dataMapper;

    /**
     * 私有构造方法
     *
     * @param pageSize     每页数据量
     * @param total        数据总数
     * @param queryWrapper 查询条件包装器
     * @param tableName    表名
     * @param dataMapper   数据映射器，用于将数据库行映射为目标数据类型
     */
    private PageIterator(int pageSize,
                         long total,
                         QueryWrapper queryWrapper,
                         String tableName,
                         Function<Map<String, Object>, T> dataMapper) {
        this.pageSize   = pageSize;
        this.total      = total;
        this.wrapper    = queryWrapper;
        this.tableName  = tableName;
        this.dataMapper = dataMapper;
    }

    /**
     * 静态工厂方法，创建分页迭代器实例
     *
     * @param <T>          数据类型
     * @param pageSize     每页数据量
     * @param total        数据总数
     * @param queryWrapper 查询条件包装器
     * @param tableName    表名
     * @return 分页迭代器实例
     */
    public static <T extends BaseSourceData> PageIterator<T> of(int pageSize, long total, QueryWrapper queryWrapper, String tableName, Function<Map<String, Object>, T> dataMapper) {
        return new PageIterator<>(pageSize, total, queryWrapper, tableName, dataMapper);
    }

    /**
     * 判断指定页码是否有下一页数据
     *
     * @param num 页码
     * @return 如果有下一页数据返回 true，否则返回 false
     */
    private boolean hasNext(long num) {
        return (num - 1) * pageSize < total;
    }

    /**
     * 判断是否有下一页数据
     *
     * @return 如果有下一页数据返回 true，否则返回 false
     */
    @Override
    public boolean hasNext() {
        return hasNext(pageNum.get());
    }

    /**
     * 获取下一页数据
     * <p>
     * 页码自动递增，查询数据库并将结果转换为指定类型的列表。
     * 如果没有更多数据，返回空列表。
     * </p>
     *
     * @return 下一页数据列表
     */
    @Override
    public List<T> next() {
        long num = pageNum.incrementAndGet();
        if (!hasNext(num)) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        Db.paginate(tableName, num, pageSize, wrapper)
          .getRecords()
          .forEach(r -> result.add(dataMapper.apply(r.toCamelKeysMap())));
        return result;
    }

    /**
     * 返回分页迭代器的字符串表示
     *
     * @return 包含迭代器状态信息的字符串
     */
    @Override
    public String toString() {
        return "PageIterator{" +
                "pageSize=" + pageSize +
                ", total=" + total +
                ", wrapper=" + wrapper +
                ", tableName='" + tableName + '\'' +
                ", pageNum=" + pageNum +
                '}';
    }
}
