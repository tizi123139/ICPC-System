package com.scuec.icpc.module.reward.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.reward.dto.RewardDtos;
import com.scuec.icpc.module.reward.service.RewardService;
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
 * 奖惩管理接口，对应接口手册第 12 章。
 * 查询权限：本人 / OFFICER+；维护权限：OFFICER+。
 */
@Tag(name = "奖惩管理")
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @Operation(summary = "分页查询奖励记录")
    @GetMapping("/rewards")
    public ApiResponse<List<RewardDtos.RewardVO>> rewards() {
        return ApiResponse.ok(rewardService.listRewards());
    }

    @Operation(summary = "新增奖励记录")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("新增奖励记录")
    @PostMapping("/rewards")
    public ApiResponse<RewardDtos.RewardVO> createReward(
            @Valid @RequestBody RewardDtos.RewardRequest request) {
        return ApiResponse.ok(rewardService.createReward(request));
    }

    @Operation(summary = "奖励记录详情")
    @GetMapping("/rewards/{rewardId}")
    public ApiResponse<RewardDtos.RewardVO> rewardDetail(@PathVariable Long rewardId) {
        return ApiResponse.ok(rewardService.rewardDetail(rewardId));
    }

    @Operation(summary = "修改奖励记录")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("修改奖励记录")
    @PutMapping("/rewards/{rewardId}")
    public ApiResponse<RewardDtos.RewardVO> updateReward(
            @PathVariable Long rewardId,
            @Valid @RequestBody RewardDtos.RewardRequest request) {
        return ApiResponse.ok(rewardService.updateReward(rewardId, request));
    }

    @Operation(summary = "删除奖励记录")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("删除奖励记录")
    @DeleteMapping("/rewards/{rewardId}")
    public ApiResponse<Void> deleteReward(@PathVariable Long rewardId) {
        rewardService.deleteReward(rewardId);
        return ApiResponse.ok();
    }

    @Operation(summary = "分页查询处分记录")
    @GetMapping("/punishments")
    public ApiResponse<List<RewardDtos.PunishmentVO>> punishments() {
        return ApiResponse.ok(rewardService.listPunishments());
    }

    @Operation(summary = "新增处分记录")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("新增处分记录")
    @PostMapping("/punishments")
    public ApiResponse<RewardDtos.PunishmentVO> createPunishment(
            @Valid @RequestBody RewardDtos.PunishmentRequest request) {
        return ApiResponse.ok(rewardService.createPunishment(request));
    }

    @Operation(summary = "处分记录详情")
    @GetMapping("/punishments/{punishmentId}")
    public ApiResponse<RewardDtos.PunishmentVO> punishmentDetail(
            @PathVariable Long punishmentId) {
        return ApiResponse.ok(rewardService.punishmentDetail(punishmentId));
    }

    @Operation(summary = "修改处分记录")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("修改处分记录")
    @PutMapping("/punishments/{punishmentId}")
    public ApiResponse<RewardDtos.PunishmentVO> updatePunishment(
            @PathVariable Long punishmentId,
            @Valid @RequestBody RewardDtos.PunishmentRequest request) {
        return ApiResponse.ok(rewardService.updatePunishment(punishmentId, request));
    }

    @Operation(summary = "删除处分记录")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("删除处分记录")
    @DeleteMapping("/punishments/{punishmentId}")
    public ApiResponse<Void> deletePunishment(@PathVariable Long punishmentId) {
        rewardService.deletePunishment(punishmentId);
        return ApiResponse.ok();
    }
}
