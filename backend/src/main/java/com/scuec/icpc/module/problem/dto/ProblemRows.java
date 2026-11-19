package com.scuec.icpc.module.problem.dto;

import lombok.Data;

/**
 * 题库模块自定义查询的扁平结果行。
 */
public final class ProblemRows {

    private ProblemRows() {
    }

    @Data
    public static class ProblemTagRow {
        private Long problemId;
        private Long tagId;
        private String name;
        private String category;
    }
}
