package com.scuec.icpc.module.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件模块响应对象。
 */
public final class FileDtos {

    private FileDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileVO {
        private Long fileId;
        private String url;
        private String filename;
        private Long size;
        private String contentType;
    }
}
