package com.scuec.icpc.module.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 集训模块请求与响应对象，字段命名与《接口手册》第 6 节保持一致。
 */
public final class CampDtos {

    private CampDtos() {
    }

    @Data
    public static class Query {

        @NotBlank(message = "集训类型不能为空")
        private String type;

        /**
         * 按集训开始时间所属年份筛选，为空表示不限。
         */
        private Integer year;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "集训名称不能为空")
        @Size(max = 255, message = "集训名称长度不能超过 255")
        private String name;

        @NotBlank(message = "集训类型不能为空")
        private String type;

        @NotNull(message = "集训开始时间不能为空")
        private LocalDateTime startTime;

        @NotNull(message = "集训结束时间不能为空")
        private LocalDateTime endTime;

        @Size(max = 255, message = "集训地点长度不能超过 255")
        private String location;

        @NotNull(message = "报名截止时间不能为空")
        private LocalDateTime registrationDeadline;

        @NotBlank(message = "集训说明不能为空")
        @Size(max = 5000, message = "集训说明长度不能超过 5000")
        private String description;

        @Size(max = 5000, message = "报名要求长度不能超过 5000")
        private String requirements;
    }

    @Data
    public static class RegistrationRequest {

        @Size(max = 1000, message = "报名理由长度不能超过 1000")
        private String reason;

        private List<LocalDate> availableDates;
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
    public static class TaskCreateRequest {

        @NotBlank(message = "任务标题不能为空")
        @Size(max = 255, message = "任务标题长度不能超过 255")
        private String title;

        @NotNull(message = "任务日期不能为空")
        private LocalDate date;

        @NotBlank(message = "任务内容不能为空")
        @Size(max = 5000, message = "任务内容长度不能超过 5000")
        private String content;

        private List<Long> problemIds;
    }

    @Data
    public static class CheckInRequest {

        @NotNull(message = "签到日期不能为空")
        private LocalDate date;

        @Size(max = 500, message = "签到备注长度不能超过 500")
        private String remark;
    }

    @Data
    public static class AttendanceQuery {
        private LocalDate date;
        private Long userId;
    }

    @Data
    public static class AttendanceUpdateRequest {

        @NotBlank(message = "考勤状态不能为空")
        private String status;

        @Size(max = 500, message = "考勤备注长度不能超过 500")
        private String remark;
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
    public static class CampVO {
        private Long campId;
        private String name;
        private String type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String location;
        private LocalDateTime registrationDeadline;
        private String description;
        private String requirements;
        private String status;
        private UserBrief creator;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProblem {
        private Long problemId;
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskVO {
        private Long taskId;
        private String title;
        private LocalDate date;
        private String content;
        private List<Long> problemIds;
        private List<TaskProblem> problems;
        /**
         * 接口文档预留附件字段，当前文件模块未提供任务附件关联，返回空列表。
         */
        private List<Object> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampDetailVO {
        private CampVO basicInfo;
        private List<TaskVO> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationVO {
        private Long applyId;
        private Long campId;
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
    public static class AttendanceVO {
        private Long recordId;
        private Long campId;
        private Long userId;
        private String userName;
        private String studentId;
        private LocalDate checkInDate;
        private LocalDateTime checkInTime;
        private String status;
        private String remark;
    }
}
