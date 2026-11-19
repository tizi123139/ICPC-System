package com.scuec.icpc.module.file.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.common.log.AuditLog;
import com.scuec.icpc.module.file.dto.FileDtos;
import com.scuec.icpc.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口，路径与《接口文档》/files 保持一致。
 */
@Tag(name = "文件服务")
@Validated
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "通用文件上传（题解附件、证书等）")
    @AuditLog("上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileDtos.FileVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.upload(file));
    }
}
