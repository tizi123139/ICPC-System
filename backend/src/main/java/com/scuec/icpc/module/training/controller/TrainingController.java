package com.scuec.icpc.module.training.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.training.dto.TrainingDtos;
import com.scuec.icpc.module.training.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 训练管理接口，路径与《接口文档》/trainings 保持一致。
 */
@Tag(name = "训练管理")
@Validated
@RestController
@RequestMapping("/api/v1/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @Operation(summary = "分页查询训练列表")
    @GetMapping
    public ApiResponse<PageResult<TrainingDtos.TrainingVO>> page(
            @Valid TrainingDtos.Query query) {
        return ApiResponse.ok(trainingService.page(query));
    }

    @Operation(summary = "创建训练")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("创建训练")
    @PostMapping
    public ApiResponse<TrainingDtos.TrainingVO> create(
            @Valid @RequestBody TrainingDtos.CreateRequest request) {
        return ApiResponse.ok(trainingService.create(request));
    }

    @Operation(summary = "获取训练详情（含题目列表）")
    @GetMapping("/{trainingId}")
    public ApiResponse<TrainingDtos.TrainingDetailVO> detail(@PathVariable Long trainingId) {
        return ApiResponse.ok(trainingService.detail(trainingId));
    }

    @Operation(summary = "修改训练（仅草稿状态可改）")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("修改训练")
    @PutMapping("/{trainingId}")
    public ApiResponse<TrainingDtos.TrainingVO> update(
            @PathVariable Long trainingId,
            @Valid @RequestBody TrainingDtos.UpdateRequest request) {
        return ApiResponse.ok(trainingService.update(trainingId, request));
    }

    @Operation(summary = "删除训练（仅草稿状态可删）")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("删除训练")
    @DeleteMapping("/{trainingId}")
    public ApiResponse<Void> delete(@PathVariable Long trainingId) {
        trainingService.delete(trainingId);
        return ApiResponse.ok();
    }

    @Operation(summary = "发布训练")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("发布训练")
    @PostMapping("/{trainingId}/publish")
    public ApiResponse<TrainingDtos.TrainingVO> publish(@PathVariable Long trainingId) {
        return ApiResponse.ok(trainingService.publish(trainingId));
    }

    @Operation(summary = "查询训练提交记录")
    @GetMapping("/{trainingId}/submissions")
    public ApiResponse<List<TrainingDtos.SubmissionVO>> submissions(
            @PathVariable Long trainingId,
            @Valid TrainingDtos.SubmissionQuery query) {
        return ApiResponse.ok(trainingService.submissions(trainingId, query));
    }

    @Operation(summary = "提交代码（训练中，判题结果异步返回）")
    @AuditLog("提交训练代码")
    @PostMapping("/{trainingId}/submit")
    public ApiResponse<TrainingDtos.SubmissionVO> submit(
            @PathVariable Long trainingId,
            @Valid @RequestBody TrainingDtos.SubmitRequest request) {
        return ApiResponse.ok(trainingService.submit(trainingId, request));
    }

    @Operation(summary = "获取训练实时榜单")
    @GetMapping("/{trainingId}/leaderboard")
    public ApiResponse<TrainingDtos.LeaderboardVO> leaderboard(@PathVariable Long trainingId) {
        return ApiResponse.ok(trainingService.leaderboard(trainingId));
    }

    @Operation(summary = "获取个人能力评估（雷达图数据）")
    @GetMapping("/ability/{userId}")
    public ApiResponse<TrainingDtos.AbilityVO> ability(@PathVariable Long userId) {
        return ApiResponse.ok(trainingService.ability(userId));
    }
}
