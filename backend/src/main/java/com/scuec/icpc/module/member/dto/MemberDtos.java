package com.scuec.icpc.module.member.dto;

import com.scuec.icpc.common.api.PageQuery;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会员模块请求与响应对象。
 */
public final class MemberDtos {

    private MemberDtos() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {
        /**
         * 姓名 / 学号模糊搜索。
         */
        private String keyword;
        private String memberType;
        private String grade;
        private String department;
        private String status;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "学号不能为空")
        @Size(max = 32, message = "学号长度不能超过 32")
        private String studentId;

        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名长度不能超过 50")
        private String name;

        @NotBlank(message = "专业不能为空")
        @Size(max = 100, message = "专业长度不能超过 100")
        private String major;

        @Size(max = 20, message = "年级长度不能超过 20")
        private String grade;

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100")
        private String email;

        @NotBlank(message = "会员类型不能为空")
        private String memberType;

        @Size(max = 50, message = "部门长度不能超过 50")
        private String department;

        private LocalDate joinDate;
    }

    @Data
    public static class UpdateRequest {

        @Size(max = 50, message = "姓名长度不能超过 50")
        private String name;

        @Size(max = 100, message = "专业长度不能超过 100")
        private String major;

        @Size(max = 20, message = "年级长度不能超过 20")
        private String grade;

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100")
        private String email;

        @Size(max = 255, message = "头像地址长度不能超过 255")
        private String avatar;

        private String memberType;
        private String department;
        private String status;
    }

    @Data
    public static class ApplyJoinRequest {

        @NotBlank(message = "学号不能为空")
        @Size(max = 32, message = "学号长度不能超过 32")
        private String studentId;

        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名长度不能超过 50")
        private String name;

        @NotBlank(message = "专业不能为空")
        @Size(max = 100, message = "专业长度不能超过 100")
        private String major;

        @NotBlank(message = "年级不能为空")
        @Size(max = 20, message = "年级长度不能超过 20")
        private String grade;

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100")
        private String email;

        @NotBlank(message = "入会动机不能为空")
        @Size(max = 1000, message = "入会动机长度不能超过 1000")
        private String reason;
    }

    @Data
    public static class ApplyResignRequest {

        @NotBlank(message = "退会原因不能为空")
        @Size(max = 1000, message = "退会原因长度不能超过 1000")
        private String reason;
    }

    @Data
    public static class ApplyQuery {
        private String type;
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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberVO {
        private Long memberId;
        private String studentId;
        private String name;
        private String major;
        private String grade;
        private String phone;
        private String email;
        private String avatar;
        private String memberType;
        private String department;
        private LocalDate joinDate;
        private String status;
        private List<String> roles;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplyVO {
        private Long applyId;
        private String type;
        private Long applicantId;
        private String applicantName;
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
    public static class ArchiveVO {
        private MemberVO basicInfo;
        private List<Map<String, Object>> trainingRecords;
        private List<Map<String, Object>> contestRecords;
        private List<Map<String, Object>> rewards;
        private List<Map<String, Object>> punishments;
    }
}
