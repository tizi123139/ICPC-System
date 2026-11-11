package com.scuec.icpc.module.export.controller;

import com.scuec.icpc.module.export.dto.ExportDtos;
import com.scuec.icpc.module.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 数据导出接口，路径与《接口文档》/export 保持一致。
 * 返回二进制流，不包裹 ApiResponse。
 */
@Tag(name = "数据导出")
@Validated
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "导出训练成绩报表（Excel）")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @GetMapping("/training-report/{trainingId}")
    public ResponseEntity<byte[]> trainingReport(@PathVariable Long trainingId) {
        return toResponse(exportService.trainingReport(trainingId));
    }

    @Operation(summary = "下载赛事获奖证书打包（ZIP）")
    @PreAuthorize("hasRole('ASSISTANT_COACH')")
    @GetMapping("/contest-certificates/{contestId}")
    public ResponseEntity<byte[]> contestCertificates(@PathVariable Long contestId) {
        return toResponse(exportService.contestCertificates(contestId));
    }

    /**
     * 中文文件名用 RFC 5987 编码，兼容各浏览器下载。
     */
    private ResponseEntity<byte[]> toResponse(ExportDtos.ExportFile file) {
        String encoded = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(file.getContent());
    }
}
