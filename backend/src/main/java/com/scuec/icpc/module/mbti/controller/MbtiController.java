package com.scuec.icpc.module.mbti.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.mbti.dto.MbtiDtos;
import com.scuec.icpc.module.mbti.service.MbtiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MBTI 测评接口，路径与《接口文档》/mbti 保持一致。
 */
@Tag(name = "MBTI 测评")
@Validated
@RestController
@RequestMapping("/api/v1/mbti")
@RequiredArgsConstructor
public class MbtiController {

    private final MbtiService mbtiService;

    @Operation(summary = "获取 MBTI 测评题目列表")
    @GetMapping("/questions")
    public ApiResponse<List<MbtiDtos.QuestionVO>> questions() {
        return ApiResponse.ok(mbtiService.questions());
    }

    @Operation(summary = "提交 MBTI 测评答案")
    @AuditLog("提交 MBTI 测评")
    @PostMapping("/submit")
    public ApiResponse<MbtiDtos.ResultVO> submit(@Valid @RequestBody MbtiDtos.SubmitRequest request) {
        return ApiResponse.ok(mbtiService.submit(request));
    }

    @Operation(summary = "查看某会员 MBTI 测评结果（取最新一次）")
    @GetMapping("/results/{userId}")
    public ApiResponse<MbtiDtos.ResultVO> result(@PathVariable Long userId) {
        return ApiResponse.ok(mbtiService.latest(userId));
    }
}
