package com.scuec.icpc.module.problem.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.problem.dto.ProblemDtos;
import com.scuec.icpc.module.problem.service.ProblemService;
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
 * 题库管理接口，路径与《接口文档》/problems 保持一致。
 */
@Tag(name = "题库管理")
@Validated
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @Operation(summary = "分页查询题目列表")
    @GetMapping
    public ApiResponse<PageResult<ProblemDtos.ProblemVO>> page(
            @Valid ProblemDtos.Query query) {
        return ApiResponse.ok(problemService.page(query));
    }

    @Operation(summary = "新增题目")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("新增题目")
    @PostMapping
    public ApiResponse<ProblemDtos.ProblemVO> create(
            @Valid @RequestBody ProblemDtos.CreateRequest request) {
        return ApiResponse.ok(problemService.create(request));
    }

    @Operation(summary = "从 OJ 批量导入题目")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("导入题目")
    @PostMapping("/import")
    public ApiResponse<ProblemDtos.ImportResult> importProblems(
            @Valid @RequestBody ProblemDtos.ImportRequest request) {
        return ApiResponse.ok(problemService.importFromOj(request));
    }

    @Operation(summary = "获取题目详情")
    @GetMapping("/{problemId}")
    public ApiResponse<ProblemDtos.ProblemVO> detail(@PathVariable Long problemId) {
        return ApiResponse.ok(problemService.detail(problemId));
    }

    @Operation(summary = "修改题目")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("修改题目")
    @PutMapping("/{problemId}")
    public ApiResponse<Void> update(@PathVariable Long problemId,
                                    @Valid @RequestBody ProblemDtos.UpdateRequest request) {
        problemService.update(problemId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除题目")
    @PreAuthorize("hasRole('MINISTER')")
    @AuditLog("删除题目")
    @DeleteMapping("/{problemId}")
    public ApiResponse<Void> delete(@PathVariable Long problemId) {
        problemService.delete(problemId);
        return ApiResponse.ok();
    }

    @Operation(summary = "更新题解")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("更新题解")
    @PutMapping("/{problemId}/solution")
    public ApiResponse<Void> updateSolution(@PathVariable Long problemId,
                                            @Valid @RequestBody ProblemDtos.SolutionRequest request) {
        problemService.updateSolution(problemId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "题库内提交代码")
    @AuditLog("题库内提交代码")
    @PostMapping("/{problemId}/submit")
    public ApiResponse<ProblemDtos.SubmitVO> submit(@PathVariable Long problemId,
                                                    @Valid @RequestBody ProblemDtos.SubmitRequest request) {
        return ApiResponse.ok(problemService.submit(problemId, request));
    }

    @Operation(summary = "题目提交记录")
    @GetMapping("/{problemId}/submissions")
    public ApiResponse<List<ProblemDtos.SubmissionVO>> submissions(@PathVariable Long problemId) {
        return ApiResponse.ok(problemService.submissions(problemId));
    }
}
