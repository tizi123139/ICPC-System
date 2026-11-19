package com.scuec.icpc.module.recommendation.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.module.recommendation.dto.RecommendationDtos;
import com.scuec.icpc.module.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个性化推荐接口，路径与《接口文档》/recommendations 保持一致。
 */
@Tag(name = "个性化推荐")
@Validated
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "基于薄弱知识点推荐习题（userId 缺省为当前用户）")
    @GetMapping("/problems")
    public ApiResponse<List<RecommendationDtos.ProblemVO>> problems(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ApiResponse.ok(recommendationService.recommendProblems(userId, limit));
    }

    @Operation(summary = "基于能力评估推荐专项训练（userId 缺省为当前用户）")
    @GetMapping("/trainings")
    public ApiResponse<List<RecommendationDtos.TrainingVO>> trainings(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        return ApiResponse.ok(recommendationService.recommendTrainings(userId, limit));
    }
}
