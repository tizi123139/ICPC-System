package com.scuec.icpc.module.camp.dto;

import lombok.Data;

/**
 * 集训模块自定义 SQL 的扁平结果行。
 */
public final class CampRows {

    private CampRows() {
    }

    @Data
    public static class TaskProblemRow {
        private Long taskId;
        private Long problemId;
        private String title;
        private Integer sortOrder;
    }
}
