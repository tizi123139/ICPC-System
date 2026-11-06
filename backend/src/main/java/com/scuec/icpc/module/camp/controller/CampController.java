package com.scuec.icpc.module.camp.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.camp.dto.CampDtos;
import com.scuec.icpc.module.camp.service.CampService;
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
 * 集训管理接口（寒假 / 暑假共用），路径与《接口文档》/camps 保持一致。
 */
@Tag(name = "集训管理")
@Validated
@RestController
@RequestMapping("/api/v1/camps")
@RequiredArgsConstructor
public class CampController {

    private final CampService campService;

    @Operation(summary = "查询集训列表（寒假/暑假）")
    @GetMapping
    public ApiResponse<List<CampDtos.CampVO>> list(@Valid CampDtos.Query query) {
        return ApiResponse.ok(campService.list(query));
    }

    @Operation(summary = "发布集训通知")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("发布集训通知")
    @PostMapping
    public ApiResponse<CampDtos.CampVO> create(
            @Valid @RequestBody CampDtos.CreateRequest request) {
        return ApiResponse.ok(campService.create(request));
    }

    @Operation(summary = "获取集训详情（含任务列表）")
    @GetMapping("/{campId}")
    public ApiResponse<CampDtos.CampDetailVO> detail(@PathVariable Long campId) {
        return ApiResponse.ok(campService.detail(campId));
    }

    @Operation(summary = "提交集训报名")
    @AuditLog("提交集训报名")
    @PostMapping("/{campId}/register")
    public ApiResponse<Void> register(@PathVariable Long campId,
                                      @Valid @RequestBody CampDtos.RegistrationRequest request) {
        campService.register(campId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "查询集训报名列表")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @GetMapping("/{campId}/registrations")
    public ApiResponse<List<CampDtos.RegistrationVO>> registrations(
            @PathVariable Long campId,
            @Valid CampDtos.RegistrationQuery query) {
        return ApiResponse.ok(campService.registrations(campId, query));
    }

    @Operation(summary = "审核集训报名")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("审核集训报名")
    @PostMapping("/{campId}/registrations/{applyId}/audit")
    public ApiResponse<Void> audit(@PathVariable Long campId,
                                   @PathVariable Long applyId,
                                   @Valid @RequestBody CampDtos.AuditRequest request) {
        campService.audit(campId, applyId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "发布集训每日任务")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("发布集训任务")
    @PostMapping("/{campId}/tasks")
    public ApiResponse<CampDtos.TaskVO> createTask(
            @PathVariable Long campId,
            @Valid @RequestBody CampDtos.TaskCreateRequest request) {
        return ApiResponse.ok(campService.createTask(campId, request));
    }

    @Operation(summary = "集训签到打卡")
    @AuditLog("集训签到")
    @PostMapping("/{campId}/checkin")
    public ApiResponse<CampDtos.AttendanceVO> checkin(
            @PathVariable Long campId,
            @Valid @RequestBody CampDtos.CheckInRequest request) {
        return ApiResponse.ok(campService.checkin(campId, request));
    }

    @Operation(summary = "查询集训考勤记录")
    @GetMapping("/{campId}/attendance")
    public ApiResponse<List<CampDtos.AttendanceVO>> attendance(
            @PathVariable Long campId,
            @Valid CampDtos.AttendanceQuery query) {
        return ApiResponse.ok(campService.attendance(campId, query));
    }

    @Operation(summary = "修改考勤状态（补签/请假/标记缺勤）")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("修改集训考勤")
    @PutMapping("/{campId}/attendance/{recordId}")
    public ApiResponse<CampDtos.AttendanceVO> updateAttendance(
            @PathVariable Long campId,
            @PathVariable Long recordId,
            @Valid @RequestBody CampDtos.AttendanceUpdateRequest request) {
        return ApiResponse.ok(campService.updateAttendance(campId, recordId, request));
    }
}
