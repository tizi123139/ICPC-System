package com.scuec.icpc.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 通用分页查询参数。page 从 1 开始，size 上限 100，避免恶意大分页拖垮数据库。
 */
@Data
public class PageQuery {

    private Integer page;
    private Integer size;

    public long pageNo() {
        return page == null || page < 1 ? 1L : page;
    }

    public long pageSize() {
        return size == null || size < 1 ? 20L : Math.min(size, 100);
    }

    public <T> Page<T> toPage() {
        return Page.of(pageNo(), pageSize());
    }
}
