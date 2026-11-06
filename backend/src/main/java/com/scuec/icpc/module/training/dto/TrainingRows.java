package com.scuec.icpc.module.training.dto;

import lombok.Data;

/**
 * 自定义统计 SQL 的扁平结果行。
 */
public final class TrainingRows {

    private TrainingRows() {
    }

    @Data
    public static class ProblemTagRow {
        private Long problemId;
        private Long tagId;
        private String name;
        private String category;
    }

    @Data
    public static class AbilityRow {
        private String tagName;
        private Integer solvedCount;
        private Integer attemptCount;
    }

    @Data
    public static class TrendRow {
        private String day;
        private Integer acCount;
        private Integer submitCount;
    }

    @Data
    public static class TeamMemberRow {
        private Long memberId;
        private Long teamId;
        private String teamName;
    }
}
