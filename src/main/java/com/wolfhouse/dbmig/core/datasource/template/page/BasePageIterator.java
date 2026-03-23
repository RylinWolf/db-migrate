package com.wolfhouse.dbmig.core.datasource.template.page;

import com.wolfhouse.dbmig.core.datasource.sourcedata.BaseSourceData;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分页迭代器父类。
 * 用于分页查询数据库表数据，支持迭代访问每一页的数据。
 *
 * @author Rylin Wolf
 */
@Accessors(fluent = true)
public abstract class BasePageIterator<T extends BaseSourceData> implements Iterator<List<T>> {
    /**
     * 每页数据量
     */
    @Getter
    protected final int        pageSize;
    /**
     * 数据总数
     */
    @Getter
    protected final long       total;
    /**
     * 当前页码，从 0 开始
     */
    @Getter
    protected final AtomicLong pageNum = new AtomicLong(0);

    public BasePageIterator(int pageSize, long total) {
        this.pageSize = pageSize;
        this.total    = total;
    }
}
