package com.scuec.icpc.module.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考核管理请求与响应对象，字段与接口手册第 13 章一致。
 */
public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentVO {
        private Long assessmentId;
        private String name;
        private String type;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer passScore;
        private String status;
        private LocalDateTime createTime;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "考核名称不能为空")
        @Size(max = 255, message = "考核名称长度不能超过 255")
        private String name;

        @NotBlank(message = "考核类型不能为空")
        private String type;

        private String description;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        private Integer passScore;
    }

    @Data
    public static class UpdateRequest {

        @Size(max = 255, message = "考核名称长度不能超过 255")
        private String name;

        private String type;

        private String description;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        private Integer passScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultVO {
        private Long resultId;
        private Long assessmentId;
        private String assessmentName;
        private Long memberId;
        private String memberName;
        private String studentId;
        private BigDecimal score;
        private String remark;
        private String auditStatus;
        private String auditRemark;
    }

    @Data
    public static class ResultItem {

        @NotNull(message = "会员不能为空")
        private Long memberId;

        @NotNull(message = "成绩不能为空")
        @DecimalMin(value = "0", message = "成绩不能为负数")
        private BigDecimal score;

        private String remark;
    }

    @Data
    public static class ResultsRequest {

        @Valid
        @NotEmpty(message = "成绩列表不能为空")
        private List<ResultItem> results;
    }

    @Data
    public static class AuditRequest {

        @NotBlank(message = "审核状态不能为空")
        private String status;

        @Size(max = 500, message = "审核意见长度不能超过 500")
        private String auditRemark;
    }
}
