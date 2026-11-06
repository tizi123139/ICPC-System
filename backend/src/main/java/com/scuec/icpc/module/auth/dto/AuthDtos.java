package com.scuec.icpc.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 认证模块请求与响应对象，字段与《接口文档》认证章节一致。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    public static class LoginRequest {

        @NotBlank(message = "学号不能为空")
        private String studentId;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String studentId;
        private String name;
        private List<String> roles;
        private String avatar;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private long expiresIn;
        private UserInfo user;
    }
}
