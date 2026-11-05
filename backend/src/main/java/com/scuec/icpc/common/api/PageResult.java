package com.scuec.icpc.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

/**
 * 分页数据结构，遵循文档 0.3 节：{ total, page, size, records }。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> mapper) {
        List<T> records = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(0, page, size, List.of());
    }
}
