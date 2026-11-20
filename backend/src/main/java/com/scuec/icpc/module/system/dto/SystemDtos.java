package com.scuec.icpc.module.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统管理请求与响应对象，字段与接口手册第 14 章一致。
 */
public final class SystemDtos {

    private SystemDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogVO {
        private Long logId;
        private Long userId;
        private String userName;
        private String action;
        private String detail;
        private String ip;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleVO {
        private String roleCode;
        private String roleName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigVO {
        private String configKey;
        private String configValue;
        private String description;
        private LocalDateTime updateTime;
    }

    @Data
    public static class ConfigItem {

        @NotBlank(message = "配置项不能为空")
        private String configKey;

        @NotBlank(message = "配置值不能为空")
        private String configValue;
    }

    @Data
    public static class ConfigsRequest {

        @Valid
        @NotEmpty(message = "配置列表不能为空")
        private List<ConfigItem> configs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileVO {
        private Long fileId;
        private String filename;
        private String url;
        private Long size;
        private String uploaderName;
        private LocalDateTime createTime;
    }

    @Data
    public static class RolesRequest {

        @NotEmpty(message = "角色列表不能为空")
        private List<String> roles;
    }
}
