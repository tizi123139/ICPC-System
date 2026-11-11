package com.scuec.icpc.module.export.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 导出模块的二进制文件载体。
 */
public final class ExportDtos {

    private ExportDtos() {
    }

    @Data
    @AllArgsConstructor
    public static class ExportFile {
        private String filename;
        private String contentType;
        private byte[] content;
    }
}
