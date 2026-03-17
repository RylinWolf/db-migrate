package com.wolfhouse.dbsync.core.datasource.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;

import java.util.Iterator;
import java.util.List;

/**
 * 分页迭代器
 *
 * @author Rylin Wolf
 */
public class PageIterator<T> implements Iterator<List<T>> {
    private final int          pageSize;
    private final long         total;
    private final QueryWrapper wrapper;
    private final ObjectMapper objectMapper;
    private final String       tableName;
    private       int          pageNum = 1;

    private PageIterator(int pageSize, long total, QueryWrapper queryWrapper, ObjectMapper objectMapper, String tableName) {
        this.pageSize     = pageSize;
        this.total        = total;
        this.wrapper      = queryWrapper;
        this.objectMapper = objectMapper;
        this.tableName    = tableName;
    }

    public static <T> PageIterator<T> of(int pageSize, long total, QueryWrapper queryWrapper, ObjectMapper objectMapper, String tableName) {
        return new PageIterator<>(pageSize, total, queryWrapper, objectMapper, tableName);
    }

    @Override
    public boolean hasNext() {
        return (long) (pageNum - 1) * pageSize < total;
    }

    @Override
    public List<T> next() {
        try {
            return objectMapper.convertValue(Db.paginate(tableName, pageNum, pageSize, wrapper).getRecords(), new TypeReference<>() {});
        } finally {
            pageNum++;
        }
    }

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
