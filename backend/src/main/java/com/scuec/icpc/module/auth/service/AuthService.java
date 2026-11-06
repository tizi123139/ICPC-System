package com.scuec.icpc.module.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.enums.Enums;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.JwtTokenProvider;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.common.security.UserPrincipal;
import com.scuec.icpc.module.auth.dto.AuthDtos;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.member.mapper.MemberRoleMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证业务：账号密码登录、登出（Redis Token 黑名单）、当前用户信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final MemberMapper memberMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        Member member = memberMapper.selectOne(Wrappers.<Member>lambdaQuery()
                .eq(Member::getStudentId, request.getStudentId().trim()));
        if (member == null || member.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw BusinessException.unauthorized("学号或密码错误");
        }
        if (!Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.forbidden("账号状态异常，无法登录");
        }

        List<String> roles = memberRoleMapper.selectRoleCodes(member.getMemberId());
        UserPrincipal principal = new UserPrincipal(member.getMemberId(), member.getStudentId(),
                member.getName(), roles);

        Member update = new Member();
        update.setMemberId(member.getMemberId());
        update.setLastLoginTime(LocalDateTime.now());
        memberMapper.updateById(update);

        String token = tokenProvider.createToken(principal);
        log.info("会员 {} 登录成功", member.getMemberId());
        return AuthDtos.LoginResponse.builder()
                .token(token)
                .expiresIn(tokenProvider.getExpirationSeconds())
                .user(toUserInfo(member, roles))
                .build();
    }

    /**
     * 将当前 Token 写入 Redis 黑名单，过期时间与 Token 剩余有效期一致。
     */
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = tokenProvider.parseToken(token);
            long ttl = tokenProvider.remainingSeconds(claims);
            if (ttl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1",
                        Duration.ofSeconds(ttl));
            }
            log.info("会员 {} 登出", tokenProvider.memberIdOf(claims));
        } catch (Exception ex) {
            // Token 已失效或格式异常时无需再拉黑，登出结果对前端仍是成功
            log.debug("登出时 Token 解析失败：{}", ex.getMessage());
        }
    }

    public AuthDtos.UserInfo profile() {
        Long memberId = SecurityUtils.currentMemberId();
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.unauthorized("账号不存在或已被删除");
        }
        return toUserInfo(member, memberRoleMapper.selectRoleCodes(memberId));
    }

    private AuthDtos.UserInfo toUserInfo(Member member, List<String> roles) {
        return AuthDtos.UserInfo.builder()
                .userId(member.getMemberId())
                .studentId(member.getStudentId())
                .name(member.getName())
                .roles(roles)
                .avatar(member.getAvatar())
                .build();
    }
}
