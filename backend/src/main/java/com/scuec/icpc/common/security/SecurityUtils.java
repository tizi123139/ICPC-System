package com.scuec.icpc.common.security;

import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

public final class SecurityUtils {

    private static final Map<String, Integer> ROLE_RANK = Map.of(
            Roles.OBSERVE_MEMBER, 1,
            Roles.MEMBER, 2,
            Roles.LAB_MEMBER, 3,
            Roles.TEAM_LEADER, 4,
            Roles.OFFICER, 5,
            Roles.ASSISTANT_COACH, 6,
            Roles.MINISTER, 7,
            Roles.PRESIDENT, 8,
            Roles.ADVISOR, 9);

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw BusinessException.unauthorized("未授权，请先登录");
        }
        return principal;
    }

    public static Long currentMemberId() {
        return currentUser().getMemberId();
    }

    public static String currentName() {
        return currentUser().getName();
    }

    public static boolean isAtLeast(String role) {
        int required = ROLE_RANK.getOrDefault(role, Integer.MAX_VALUE);
        return currentUser().getRoles().stream()
                .mapToInt(r -> ROLE_RANK.getOrDefault(r, 0))
                .anyMatch(rank -> rank >= required);
    }

    public static boolean isSelf(Long memberId) {
        return memberId != null && memberId.equals(currentMemberId());
    }

    /**
     * 本人或达到指定角色才允许访问。
     */
    public static void requireSelfOrAtLeast(Long memberId, String role) {
        if (!isSelf(memberId) && !isAtLeast(role)) {
            throw BusinessException.forbidden("无权访问该资源");
        }
    }

    public static List<String> currentRoles() {
        return currentUser().getRoles();
    }
}
