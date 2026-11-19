package com.scuec.icpc.module.mbti.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MBTI 模块请求与响应对象。
 */
public final class MbtiDtos {

    private MbtiDtos() {
    }

    /**
     * 题目选项：option 为 A/B，dimension 为单字母方向，value 为该选项权重。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionVO {
        private String option;
        private String dimension;
        private Integer value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionVO {
        private Long questionId;
        private String content;
        private List<OptionVO> options;
    }

    @Data
    public static class Answer {
        @NotNull(message = "题目 ID 不能为空")
        private Long questionId;

        @NotBlank(message = "选项不能为空")
        @Pattern(regexp = "^[AaBb]$", message = "选项只能是 A 或 B")
        private String option;
    }

    @Data
    public static class SubmitRequest {
        @NotEmpty(message = "答案列表不能为空")
        @Valid
        private List<Answer> answers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultVO {
        private Long resultId;
        private Long userId;
        private String type;
        /**
         * 各维度得分，key 为 E_I / S_N / T_F / J_P，value 为 A 侧占比百分比。
         */
        private Map<String, Double> scores;
        /**
         * 文本报告。接口文档中的 reportUrl 暂未落地文件存储，固定返回 null。
         */
        private String report;
        private String reportUrl;
        private LocalDateTime testTime;
    }
}
