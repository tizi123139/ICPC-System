package com.scuec.icpc.module.recommendation.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐模块的查询结果行。
 */
public final class RecommendationRows {

    private RecommendationRows() {
    }

    /**
     * 薄弱知识点：solvedCount 为该会员已通过题数，totalCount 为该知识点下题目总数。
     */
    @Data
    public static class WeakTagRow {
        private Long tagId;
        private String tagName;
        private Integer solvedCount;
        private Integer totalCount;
    }

    @Data
    public static class ProblemRow {
        private Long problemId;
        private String title;
        private String ojProblemId;
        private String difficulty;
        private String tagName;
    }

    @Data
    public static class TrainingRow {
        private Long trainingId;
        private String name;
        private String type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
    }
}
