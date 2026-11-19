package com.scuec.icpc.module.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 推荐模块响应对象。
 */
public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemVO {
        private Long problemId;
        private String title;
        private String ojProblemId;
        private String difficulty;
        /**
         * 命中的薄弱知识点名称。
         */
        private String matchedTag;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingVO {
        private Long trainingId;
        private String name;
        private String type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
    }
}
