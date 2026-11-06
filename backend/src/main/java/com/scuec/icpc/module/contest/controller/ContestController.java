package com.scuec.icpc.module.contest.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.contest.dto.ContestDtos;
import com.scuec.icpc.module.contest.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 赛事管理接口：校赛 / XCPC / 非 XCPC 统一入口。
 */
@Tag(name = "赛事管理")
@Validated
@RestController
@RequestMapping("/api/v1/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @Operation(summary = "查询赛事列表")
    @GetMapping
    public ApiResponse<PageResult<ContestDtos.ContestVO>> list(
            @Valid ContestDtos.Query query) {
        return ApiResponse.ok(contestService.list(query));
    }

    @Operation(summary = "创建赛事")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("创建赛事")
    @PostMapping
    public ApiResponse<ContestDtos.ContestVO> create(
            @Valid @RequestBody ContestDtos.CreateRequest request) {
        return ApiResponse.ok(contestService.create(request));
    }

    @Operation(summary = "获取赛事详情")
    @GetMapping("/{contestId}")
    public ApiResponse<ContestDtos.ContestVO> detail(@PathVariable Long contestId) {
        return ApiResponse.ok(contestService.detail(contestId));
    }

    @Operation(summary = "查询赛事报名列表")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @GetMapping("/{contestId}/registrations")
    public ApiResponse<List<ContestDtos.RegistrationVO>> registrations(
            @PathVariable Long contestId,
            @Valid ContestDtos.RegistrationQuery query) {
        return ApiResponse.ok(contestService.registrations(contestId, query));
    }

    @Operation(summary = "个人/队伍报名赛事")
    @AuditLog("报名赛事")
    @PostMapping("/{contestId}/register")
    public ApiResponse<Void> register(@PathVariable Long contestId,
                                      @Valid @RequestBody ContestDtos.RegisterRequest request) {
        contestService.register(contestId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "审核赛事报名")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("审核赛事报名")
    @PostMapping("/{contestId}/registrations/{applyId}/audit")
    public ApiResponse<Void> audit(@PathVariable Long contestId,
                                   @PathVariable Long applyId,
                                   @Valid @RequestBody ContestDtos.AuditRequest request) {
        contestService.audit(contestId, applyId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "查看赛事实时榜单")
    @GetMapping("/{contestId}/leaderboard")
    public ApiResponse<ContestDtos.LeaderboardVO> leaderboard(
            @PathVariable Long contestId) {
        return ApiResponse.ok(contestService.leaderboard(contestId));
    }

    @Operation(summary = "提交赛题代码")
    @AuditLog("提交赛题代码")
    @PostMapping("/{contestId}/submit")
    public ApiResponse<ContestDtos.SubmitVO> submit(
            @PathVariable Long contestId,
            @Valid @RequestBody ContestDtos.SubmitRequest request) {
        return ApiResponse.ok(contestService.submit(contestId, request));
    }

    @Operation(summary = "按奖项批量生成证书")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("生成赛事证书")
    @PostMapping("/{contestId}/certificates")
    public ApiResponse<ContestDtos.CertificateVO> certificates(
            @PathVariable Long contestId,
            @Valid @RequestBody ContestDtos.CertificateRequest request) {
        return ApiResponse.ok(contestService.certificates(contestId, request));
    }

    @Operation(summary = "登记赛事最终成绩")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("登记赛事成绩")
    @PutMapping("/{contestId}/results")
    public ApiResponse<Void> saveResults(
            @PathVariable Long contestId,
            @Valid @RequestBody ContestDtos.ResultRequest request) {
        contestService.saveResults(contestId, request);
        return ApiResponse.ok();
    }
}
