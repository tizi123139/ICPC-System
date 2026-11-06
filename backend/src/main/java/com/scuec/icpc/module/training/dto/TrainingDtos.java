package com.scuec.icpc.module.training.dto;

import com.scuec.icpc.common.api.PageQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练模块请求与响应对象。
 */
public final class TrainingDtos {

    private TrainingDtos() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {
        private String type;
        private String status;
        /**
         * 训练名称模糊搜索。
         */
        private String keyword;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "训练名称不能为空")
        @Size(max = 255, message = "训练名称长度不能超过 255")
        private String name;

        @NotBlank(message = "训练类型不能为空")
        private String type;

        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;

        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;

        @NotEmpty(message = "训练题目不能为空")
        private List<Long> problemIds;

        private Boolean leaderboardVisible = Boolean.TRUE;

        @Min(value = 0, message = "封榜时间不能小于 0")
        private Integer freezeTime = 0;

        @Size(max = 2000, message = "训练说明长度不能超过 2000")
        private String description;
    }

    @Data
    public static class UpdateRequest {

        @Size(max = 255, message = "训练名称长度不能超过 255")
        private String name;

        private LocalDateTime startTime;
        private LocalDateTime endTime;

        /**
         * 为空表示不修改题目关联。
         */
        private List<Long> problemIds;

        private Boolean leaderboardVisible;

        @Min(value = 0, message = "封榜时间不能小于 0")
        private Integer freezeTime;

        @Size(max = 2000, message = "训练说明长度不能超过 2000")
        private String description;
    }

    @Data
    public static class SubmitRequest {

        @NotNull(message = "题目 ID 不能为空")
        private Long problemId;

        @NotBlank(message = "代码不能为空")
        @Size(max = 65535, message = "代码长度不能超过 65535")
        private String code;

        @NotBlank(message = "编程语言不能为空")
        @Size(max = 30, message = "编程语言长度不能超过 30")
        private String language;
    }

    @Data
    public static class SubmissionQuery {
        private Long userId;
        private Long problemId;
        private String result;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBrief {
        private Long memberId;
        private String studentId;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingVO {
        private Long trainingId;
        private String name;
        private String type;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean leaderboardVisible;
        private Integer freezeTime;
        private List<Long> problemIds;
        private String status;
        private UserBrief creator;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagItem {
        private Long tagId;
        private String name;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemItem {
        private Long problemId;
        private String title;
        private String ojProblemId;
        private String difficulty;
        private Integer timeLimit;
        private Integer memoryLimit;
        private String description;
        private String inputFormat;
        private String outputFormat;
        private String sampleInput;
        private String sampleOutput;
        private String hint;
        private List<TagItem> tags;
        private String visibility;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingDetailVO {
        private TrainingVO basicInfo;
        private List<ProblemItem> problems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionVO {
        private Long submitId;
        private Long trainingId;
        private Long problemId;
        private String problemTitle;
        private Long submitterId;
        private String submitterName;
        private String code;
        private String language;
        private String result;
        private Integer timeUsed;
        private Integer memoryUsed;
        private LocalDateTime submitTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private Integer rank;
        private Long userId;
        private String userName;
        private Long teamId;
        private String teamName;
        private Integer solvedCount;
        private Long penalty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardVO {
        private Boolean frozen;
        private List<LeaderboardEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityDimension {
        private String tagName;
        private Double score;
        private Integer solvedCount;
        private Integer attemptCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String day;
        private Integer acCount;
        private Integer submitCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityVO {
        private List<AbilityDimension> tagScores;
        private List<TrendPoint> recentTrend;
        private String report;
        private Integer totalSolved;
    }
}
