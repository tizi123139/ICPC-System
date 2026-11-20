package com.scuec.icpc.module.assessment.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.assessment.dto.AssessmentDtos;
import com.scuec.icpc.module.assessment.service.AssessmentService;
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
 * 考核管理接口，对应接口手册第 13 章。
 */
@Tag(name = "考核管理")
@Validated
@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @Operation(summary = "分页查询考核（普通会员仅见已发布）")
    @GetMapping
    public ApiResponse<List<AssessmentDtos.AssessmentVO>> list() {
        return ApiResponse.ok(assessmentService.list());
    }

    @Operation(summary = "创建考核")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("创建考核")
    @PostMapping
    public ApiResponse<AssessmentDtos.AssessmentVO> create(
            @Valid @RequestBody AssessmentDtos.CreateRequest request) {
        return ApiResponse.ok(assessmentService.create(request));
    }

    @Operation(summary = "考核详情")
    @GetMapping("/{assessmentId}")
    public ApiResponse<AssessmentDtos.AssessmentVO> detail(@PathVariable Long assessmentId) {
        return ApiResponse.ok(assessmentService.detail(assessmentId));
    }

    @Operation(summary = "修改考核（仅 DRAFT）")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("修改考核")
    @PutMapping("/{assessmentId}")
    public ApiResponse<AssessmentDtos.AssessmentVO> update(
            @PathVariable Long assessmentId,
            @Valid @RequestBody AssessmentDtos.UpdateRequest request) {
        return ApiResponse.ok(assessmentService.update(assessmentId, request));
    }

    @Operation(summary = "删除考核")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("删除考核")
    @DeleteMapping("/{assessmentId}")
    public ApiResponse<Void> delete(@PathVariable Long assessmentId) {
        assessmentService.delete(assessmentId);
        return ApiResponse.ok();
    }

    @Operation(summary = "发布考核")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("发布考核")
    @PostMapping("/{assessmentId}/publish")
    public ApiResponse<Void> publish(@PathVariable Long assessmentId) {
        assessmentService.publish(assessmentId);
        return ApiResponse.ok();
    }

    @Operation(summary = "查询考核结果")
    @GetMapping("/{assessmentId}/results")
    public ApiResponse<List<AssessmentDtos.ResultVO>> results(@PathVariable Long assessmentId) {
        return ApiResponse.ok(assessmentService.results(assessmentId));
    }

    @Operation(summary = "批量录入 / 更新考核成绩")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("录入考核成绩")
    @PutMapping("/{assessmentId}/results")
    public ApiResponse<Void> updateResults(
            @PathVariable Long assessmentId,
            @Valid @RequestBody AssessmentDtos.ResultsRequest request) {
        assessmentService.updateResults(assessmentId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "审核考核结果")
    @PreAuthorize("hasRole('PRESIDENT')")
    @AuditLog("审核考核结果")
    @PostMapping("/{assessmentId}/results/{resultId}/audit")
    public ApiResponse<Void> auditResult(
            @PathVariable Long assessmentId,
            @PathVariable Long resultId,
            @Valid @RequestBody AssessmentDtos.AuditRequest request) {
        assessmentService.auditResult(assessmentId, resultId, request);
        return ApiResponse.ok();
    }
}
