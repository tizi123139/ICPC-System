package com.scuec.icpc.module.system.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.system.dto.SystemDtos;
import com.scuec.icpc.module.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统管理接口，对应接口手册第 14 章。
 * 权限：会长/副会长、指导老师（ADVISOR 通过角色层级继承 PRESIDENT）。
 */
@Tag(name = "系统管理")
@Validated
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PRESIDENT')")
public class SystemController {

    private final SystemService systemService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/logs")
    public ApiResponse<List<SystemDtos.LogVO>> logs() {
        return ApiResponse.ok(systemService.logs());
    }

    @Operation(summary = "操作日志详情")
    @GetMapping("/logs/{logId}")
    public ApiResponse<SystemDtos.LogVO> logDetail(@PathVariable Long logId) {
        return ApiResponse.ok(systemService.logDetail(logId));
    }

    @Operation(summary = "查询角色字典")
    @GetMapping("/roles")
    public ApiResponse<List<SystemDtos.RoleVO>> roles() {
        return ApiResponse.ok(systemService.roles());
    }

    @Operation(summary = "调整会员角色")
    @AuditLog("调整会员角色")
    @PutMapping("/members/{memberId}/roles")
    public ApiResponse<Void> updateMemberRoles(@PathVariable Long memberId,
                                               @Valid @RequestBody SystemDtos.RolesRequest request) {
        systemService.updateMemberRoles(memberId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "查询系统配置")
    @GetMapping("/configs")
    public ApiResponse<List<SystemDtos.ConfigVO>> configs() {
        return ApiResponse.ok(systemService.configs());
    }

    @Operation(summary = "批量更新系统配置")
    @AuditLog("更新系统配置")
    @PutMapping("/configs")
    public ApiResponse<Void> updateConfigs(@Valid @RequestBody SystemDtos.ConfigsRequest request) {
        systemService.updateConfigs(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "分页查询文件记录")
    @GetMapping("/files")
    public ApiResponse<List<SystemDtos.FileVO>> files() {
        return ApiResponse.ok(systemService.files());
    }

    @Operation(summary = "删除文件记录")
    @AuditLog("删除文件记录")
    @DeleteMapping("/files/{fileId}")
    public ApiResponse<Void> deleteFile(@PathVariable Long fileId) {
        systemService.deleteFile(fileId);
        return ApiResponse.ok();
    }
}
