package com.scuec.icpc.module.auth.controller;

import com.scuec.icpc.common.api.ApiResponse;
import com.scuec.icpc.module.auth.dto.AuthDtos;
import com.scuec.icpc.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、登出、当前用户信息。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return ApiResponse.ok();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/profile")
    public ApiResponse<AuthDtos.UserInfo> profile() {
        return ApiResponse.ok(authService.profile());
    }
}
