package com.scuec.icpc.module.file.service;

import com.scuec.icpc.common.config.UploadProperties;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.file.dto.FileDtos;
import com.scuec.icpc.module.file.entity.FileRecord;
import com.scuec.icpc.module.file.mapper.FileRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 通用文件上传：按 yyyy/MM 分目录存储，白名单校验扩展名，并记录到 file_record。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    /**
     * 允许上传的扩展名白名单，避免可执行文件落盘。
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "pdf", "zip", "txt", "md", "csv",
            "xlsx", "xls", "doc", "docx", "ppt", "pptx");

    private static final DateTimeFormatter DIRECTORY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final FileRecordMapper fileRecordMapper;
    private final UploadProperties uploadProperties;

    @Transactional
    public FileDtos.FileVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("上传文件不能为空");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw BusinessException.badRequest("文件大小超过限制（最大 "
                    + (uploadProperties.getMaxSize() / 1024 / 1024) + " MB）");
        }

        String originalName = resolveOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw BusinessException.badRequest("不支持的文件类型：" + extension);
        }

        Path root = Paths.get(uploadProperties.getDirectory()).toAbsolutePath().normalize();
        String relativeDir = LocalDate.now().format(DIRECTORY_FORMAT);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = root.resolve(relativeDir).resolve(storedName).normalize();
        // 规范化后必须仍位于上传根目录内，防止 ../ 路径穿越
        if (!target.startsWith(root)) {
            throw BusinessException.badRequest("非法的文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            log.error("保存上传文件失败：{}", target, ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }

        FileRecord record = new FileRecord();
        record.setFilename(originalName);
        record.setStoredName(storedName);
        record.setUrl(uploadProperties.getUrlPrefix() + "/" + relativeDir + "/" + storedName);
        record.setSize(file.getSize());
        record.setContentType(file.getContentType());
        record.setUploaderId(SecurityUtils.currentMemberId());
        fileRecordMapper.insert(record);

        log.info("会员 {} 上传文件 {}（{} 字节）",
                record.getUploaderId(), originalName, file.getSize());
        return toVO(record);
    }

    // ==================== 内部工具 ====================

    /**
     * 浏览器可能带上完整路径，这里只保留文件名部分。
     */
    private String resolveOriginalName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw BusinessException.badRequest("文件名不能为空");
        }
        String normalized = rawName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(name)) {
            throw BusinessException.badRequest("文件名不能为空");
        }
        return name;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw BusinessException.badRequest("文件缺少扩展名");
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private FileDtos.FileVO toVO(FileRecord record) {
        return FileDtos.FileVO.builder()
                .fileId(record.getFileId())
                .url(record.getUrl())
                .filename(record.getFilename())
                .size(record.getSize())
                .contentType(record.getContentType())
                .build();
    }
}
