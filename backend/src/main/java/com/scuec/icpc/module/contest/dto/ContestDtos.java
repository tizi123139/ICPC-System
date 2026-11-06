package com.scuec.icpc.module.contest.dto;

import com.scuec.icpc.common.api.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 赛事模块请求与响应对象，字段与《接口文档》赛事管理章节一致。
 */
public final class ContestDtos {

    private ContestDtos() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {
        private String category;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "赛事名称不能为空")
        @Size(max = 255, message = "赛事名称长度不能超过 255")
        private String name;

        @NotBlank(message = "赛事类别不能为空")
        private String category;

        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;

        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;

        @NotNull(message = "报名截止时间不能为空")
        private LocalDateTime registrationDeadline;

        @Size(max = 5000, message = "赛事说明长度不能超过 5000")
        private String description;

        @Size(max = 5000, message = "报名要求长度不能超过 5000")
        private String requirements;

        @Min(value = 1, message = "队伍人数上限至少为 1")
        @Max(value = 10, message = "队伍人数上限不能超过 10")
        private Integer maxTeamSize;

        private List<Long> problemIds;

        @Min(value = 0, message = "封榜时间不能为负数")
        private Integer freezeMinutes;
    }

    @Data
    public static class RegisterRequest {

        private Long teamId;

        private List<String> materials;

        @Size(max = 500, message = "备注长度不能超过 500")
        private String remark;
    }

    @Data
    public static class RegistrationQuery {
        private String status;
    }

    @Data
    public static class AuditRequest {

        @NotBlank(message = "审批动作不能为空")
        private String action;

        @Size(max = 500, message = "审批备注长度不能超过 500")
        private String remark;
    }

    @Data
    public static class SubmitRequest {

        @NotNull(message = "题目 ID 不能为空")
        private Long problemId;

        @NotBlank(message = "代码不能为空")
        private String code;

        @NotBlank(message = "编程语言不能为空")
        private String language;
    }

    @Data
    public static class AwardRule {

        @NotBlank(message = "奖项名称不能为空")
        private String awardName;

        @NotBlank(message = "获奖名次区间不能为空")
        private String rankRange;
    }

    @Data
    public static class CertificateRequest {
        private Long templateId;
        private List<AwardRule> awardRules;
    }

    @Data
    public static class ResultItem {
        private Long teamId;
        private Integer rank;
        private String award;
        private BigDecimal score;
    }

    @Data
    public static class ResultRequest {

        @NotNull(message = "成绩列表不能为空")
        private List<ResultItem> results;
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
    public static class ContestVO {
        private Long contestId;
        private String name;
        private String category;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime registrationDeadline;
        private String description;
        private String requirements;
        private Integer maxTeamSize;
        private Integer freezeMinutes;
        private String status;
        private List<Long> problemIds;
        private UserBrief creator;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationVO {
        private Long applyId;
        private Long contestId;
        private Long teamId;
        private String teamName;
        private Long applicantId;
        private String applicantName;
        private String studentId;
        private Map<String, Object> applyData;
        private String status;
        private String auditRemark;
        private Long auditorId;
        private String auditorName;
        private LocalDateTime applyTime;
        private LocalDateTime auditTime;
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
        private Integer penalty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardVO {
        private boolean frozen;
        private List<LeaderboardEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitVO {
        private Long submitId;
        private String result;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateVO {
        private Integer generatedCount;
        private String downloadUrl;
    }
}
