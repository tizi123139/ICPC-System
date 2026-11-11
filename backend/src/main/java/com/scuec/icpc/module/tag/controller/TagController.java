package com.scuec.icpc.module.tag.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.tag.dto.TagDtos;
import com.scuec.icpc.module.tag.service.TagService;
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
 * 知识点管理接口，路径与《接口文档》/tags 保持一致。
 */
@Tag(name = "知识点管理")
@Validated
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "获取知识点树")
    @GetMapping
    public ApiResponse<List<TagDtos.TagVO>> tree() {
        return ApiResponse.ok(tagService.tree());
    }

    @Operation(summary = "新增知识点")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("新增知识点")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody TagDtos.CreateRequest request) {
        return ApiResponse.ok(tagService.create(request));
    }

    @Operation(summary = "修改知识点")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("修改知识点")
    @PutMapping("/{tagId}")
    public ApiResponse<Void> update(@PathVariable Long tagId,
                                    @Valid @RequestBody TagDtos.UpdateRequest request) {
        tagService.update(tagId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除知识点")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @AuditLog("删除知识点")
    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> delete(@PathVariable Long tagId) {
        tagService.delete(tagId);
        return ApiResponse.ok();
    }
}
