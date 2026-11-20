package com.scuec.icpc.module.reward.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 奖惩管理请求与响应对象，字段与接口手册第 12 章一致。
 */
public final class RewardDtos {

    private RewardDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardVO {
        private Long rewardId;
        private Long memberId;
        private String memberName;
        private String studentId;
        private String title;
        private String category;
        private String level;
        private BigDecimal amount;
        private String description;
        private LocalDate grantDate;
        private String issuerName;
        private LocalDateTime createTime;
    }

    @Data
    public static class RewardRequest {

        @NotNull(message = "会员不能为空")
        private Long memberId;

        @NotBlank(message = "奖励名称不能为空")
        @Size(max = 255, message = "奖励名称长度不能超过 255")
        private String title;

        @NotBlank(message = "奖励类别不能为空")
        private String category;

        @NotBlank(message = "奖励级别不能为空")
        private String level;

        @DecimalMin(value = "0", message = "金额不能为负数")
        private BigDecimal amount;

        private String description;

        @NotNull(message = "颁发日期不能为空")
        private LocalDate grantDate;

        @Size(max = 100, message = "颁发单位长度不能超过 100")
        private String issuerName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PunishmentVO {
        private Long punishmentId;
        private Long memberId;
        private String memberName;
        private String studentId;
        private String type;
        private String reason;
        private String description;
        private LocalDate effectiveDate;
        private LocalDate expireDate;
        private String issuerName;
        private LocalDateTime createTime;
    }

    @Data
    public static class PunishmentRequest {

        @NotNull(message = "会员不能为空")
        private Long memberId;

        @NotBlank(message = "处分类型不能为空")
        private String type;

        @NotBlank(message = "处分事由不能为空")
        @Size(max = 500, message = "处分事由长度不能超过 500")
        private String reason;

        private String description;

        @NotNull(message = "生效日期不能为空")
        private LocalDate effectiveDate;

        private LocalDate expireDate;

        @Size(max = 100, message = "处分单位长度不能超过 100")
        private String issuerName;
    }
}
