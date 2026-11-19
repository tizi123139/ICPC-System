package com.scuec.icpc.module.problem.dto;

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
 * 题库模块请求与响应对象，字段与《接口文档》题目管理章节一致。
 */
public final class ProblemDtos {

    private ProblemDtos() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {

        /**
         * 题目标题模糊搜索。
         */
        private String keyword;

        /**
         * 按知识点筛选。
         */
        private Long tagId;

        private String difficulty;

        private String visibility;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "题目标题不能为空")
        @Size(max = 255, message = "题目标题长度不能超过 255")
        private String title;

        @Size(max = 64, message = "OJ 题目编号长度不能超过 64")
        private String ojProblemId;

        @NotBlank(message = "题目难度不能为空")
        private String difficulty;

        @NotNull(message = "时间限制不能为空")
        @Min(value = 1, message = "时间限制必须大于 0")
        private Integer timeLimit;

        @NotNull(message = "内存限制不能为空")
        @Min(value = 1, message = "内存限制必须大于 0")
        private Integer memoryLimit;

        /**
         * 题目描述（Markdown）。允许空字符串，但不能为 null。
         */
        @NotNull(message = "题目描述不能为空")
        private String description;

        @NotNull(message = "输入格式不能为空")
        private String inputFormat;

        @NotNull(message = "输出格式不能为空")
        private String outputFormat;

        @NotNull(message = "样例输入不能为空")
        private String sampleInput;

        @NotNull(message = "样例输出不能为空")
        private String sampleOutput;

        private String hint;

        private List<Long> tagIds;

        private String visibility;
    }

    @Data
    public static class UpdateRequest {

        @Size(max = 255, message = "题目标题长度不能超过 255")
        private String title;

        @Size(max = 64, message = "OJ 题目编号长度不能超过 64")
        private String ojProblemId;

        private String difficulty;

        @Min(value = 1, message = "时间限制必须大于 0")
        private Integer timeLimit;

        @Min(value = 1, message = "内存限制必须大于 0")
        private Integer memoryLimit;

        private String description;
        private String inputFormat;
        private String outputFormat;
        private String sampleInput;
        private String sampleOutput;
        private String hint;

        /**
         * 为空表示不修改知识点关联。
         */
        private List<Long> tagIds;

        private String visibility;
    }

    @Data
    public static class SolutionRequest {

        @NotBlank(message = "题解内容不能为空")
        private String content;

        private String referenceCode;
    }

    @Data
    public static class ImportRequest {

        @NotEmpty(message = "待导入的 OJ 题目编号不能为空")
        private List<String> ojProblemIds;
    }

    @Data
    public static class SubmitRequest {

        @NotBlank(message = "代码不能为空")
        @Size(max = 65535, message = "代码长度不能超过 65535")
        private String code;

        @NotBlank(message = "编程语言不能为空")
        @Size(max = 30, message = "编程语言长度不能超过 30")
        private String language;
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
    public static class SolutionVO {
        private Long solutionId;
        private String content;
        private String referenceCode;
        private LocalDateTime updateTime;
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
        private Integer timeLimit;
        private Integer memoryLimit;
        private String description;
        private String inputFormat;
        private String outputFormat;
        private String sampleInput;
        private String sampleOutput;
        private String hint;
        private List<TagItem> tags;
        private SolutionVO solution;
        private String visibility;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitVO {
        private Long submitId;
        private String result;
    }

    /**
     * OJ 批量导入结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportResult {
        private int imported;
        private int successCount;
        private int failedCount;
    }

    /**
     * 提交人信息，与前端 SubmitRecord.submitter 对齐。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitterVO {
        private Long userId;
        private String studentId;
        private String name;
        private List<String> roles;
    }

    /**
     * 题库内提交记录，与前端 SubmitRecord 对齐。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionVO {
        private Long submitId;
        private Long trainingId;
        private Long problemId;
        private SubmitterVO submitter;
        private String language;
        private String result;
        private Integer timeUsed;
        private Integer memoryUsed;
        private LocalDateTime submitTime;
    }
}
