package com.scuec.icpc.module.statistics.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.module.statistics.dto.StatisticsDtos;
import com.scuec.icpc.module.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 数据统计接口，路径与《接口文档》/statistics 保持一致。
 */
@Tag(name = "数据统计")
@Validated
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "会员结构统计（按年级/专业/部门/类型）")
    @PreAuthorize("hasRole('OFFICER')")
    @GetMapping("/members/overview")
    public ApiResponse<StatisticsDtos.MemberOverviewVO> membersOverview() {
        return ApiResponse.ok(statisticsService.membersOverview());
    }

    @Operation(summary = "训练数据汇总（干事及以上）")
    @PreAuthorize("hasRole('OFFICER')")
    @GetMapping("/training/summary")
    public ApiResponse<StatisticsDtos.TrainingSummaryVO> trainingSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(statisticsService.trainingSummary(startDate, endDate));
    }
}
