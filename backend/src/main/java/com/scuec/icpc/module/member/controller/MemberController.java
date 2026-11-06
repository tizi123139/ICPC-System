package com.scuec.icpc.module.member.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.member.dto.MemberDtos;
import com.scuec.icpc.module.member.service.MemberService;
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
 * 会员管理接口，路径与《接口文档》/members 保持一致。
 */
@Tag(name = "会员管理")
@Validated
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "分页查询会员列表")
    @GetMapping
    public ApiResponse<PageResult<MemberDtos.MemberVO>> page(@Valid MemberDtos.Query query) {
        return ApiResponse.ok(memberService.page(query));
    }

    @Operation(summary = "新增会员（管理员）")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("新增会员")
    @PostMapping
    public ApiResponse<MemberDtos.MemberVO> create(@Valid @RequestBody MemberDtos.CreateRequest request) {
        return ApiResponse.ok(memberService.create(request));
    }

    @Operation(summary = "获取会员详情")
    @GetMapping("/{memberId}")
    public ApiResponse<MemberDtos.MemberVO> detail(@PathVariable Long memberId) {
        return ApiResponse.ok(memberService.detail(memberId));
    }

    @Operation(summary = "更新会员信息")
    @AuditLog("更新会员信息")
    @PutMapping("/{memberId}")
    public ApiResponse<MemberDtos.MemberVO> update(@PathVariable Long memberId,
                                                   @Valid @RequestBody MemberDtos.UpdateRequest request) {
        return ApiResponse.ok(memberService.update(memberId, request));
    }

    @Operation(summary = "删除会员（逻辑删除）")
    @PreAuthorize("hasRole('PRESIDENT')")
    @AuditLog("删除会员")
    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> delete(@PathVariable Long memberId) {
        memberService.delete(memberId);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取会员完整档案")
    @GetMapping("/{memberId}/archive")
    public ApiResponse<MemberDtos.ArchiveVO> archive(@PathVariable Long memberId) {
        return ApiResponse.ok(memberService.archive(memberId));
    }

    @Operation(summary = "提交入会申请")
    @PostMapping("/apply/join")
    public ApiResponse<Void> applyJoin(@Valid @RequestBody MemberDtos.ApplyJoinRequest request) {
        memberService.applyJoin(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "提交退会申请")
    @AuditLog("提交退会申请")
    @PostMapping("/apply/resign")
    public ApiResponse<Void> applyResign(@Valid @RequestBody MemberDtos.ApplyResignRequest request) {
        memberService.applyResign(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "查询待审批申请列表")
    @PreAuthorize("hasRole('OFFICER')")
    @GetMapping("/approvals")
    public ApiResponse<List<MemberDtos.ApplyVO>> approvals(@Valid MemberDtos.ApplyQuery query) {
        return ApiResponse.ok(memberService.listApprovals(query));
    }

    @Operation(summary = "审批入会/退会申请")
    @PreAuthorize("hasRole('OFFICER')")
    @AuditLog("审批会员申请")
    @PostMapping("/approvals/{applyId}/audit")
    public ApiResponse<Void> audit(@PathVariable Long applyId,
                                   @Valid @RequestBody MemberDtos.AuditRequest request) {
        memberService.audit(applyId, request);
        return ApiResponse.ok();
    }
}
