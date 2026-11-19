package com.scuec.icpc.module.team.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.team.dto.TeamDtos;
import com.scuec.icpc.module.team.service.TeamService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组队管理接口，路径与《接口文档》/teams 保持一致。
 */
@Tag(name = "组队管理")
@Validated
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "分页查询队伍列表")
    @GetMapping
    public ApiResponse<PageResult<TeamDtos.TeamVO>> page(@Valid TeamDtos.Query query) {
        return ApiResponse.ok(teamService.page(query));
    }

    @Operation(summary = "创建队伍（发起组队申请）")
    @AuditLog("创建队伍")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody TeamDtos.CreateRequest request) {
        return ApiResponse.ok(teamService.create(request));
    }

    @Operation(summary = "获取队伍详情（含成员信息与能力画像）")
    @GetMapping("/{teamId}")
    public ApiResponse<TeamDtos.TeamDetailVO> detail(@PathVariable Long teamId) {
        return ApiResponse.ok(teamService.detail(teamId));
    }

    @Operation(summary = "更新队伍信息（可转让队长）")
    @AuditLog("更新队伍信息")
    @PutMapping("/{teamId}")
    public ApiResponse<Void> update(@PathVariable Long teamId,
                                    @Valid @RequestBody TeamDtos.UpdateRequest request) {
        teamService.update(teamId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "邀请成员加入队伍")
    @AuditLog("邀请队伍成员")
    @PostMapping("/{teamId}/members")
    public ApiResponse<Void> invite(@PathVariable Long teamId,
                                    @Valid @RequestBody TeamDtos.MemberRequest request) {
        teamService.invite(teamId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "移除成员 / 成员离队")
    @AuditLog("移除队伍成员")
    @DeleteMapping("/{teamId}/members")
    public ApiResponse<Void> removeMember(@PathVariable Long teamId,
                                          @RequestParam Long memberId) {
        teamService.removeMember(teamId, memberId);
        return ApiResponse.ok();
    }

    @Operation(summary = "教练确认队伍（审核通过）")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("确认队伍")
    @PostMapping("/{teamId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long teamId) {
        teamService.confirm(teamId);
        return ApiResponse.ok();
    }
}
