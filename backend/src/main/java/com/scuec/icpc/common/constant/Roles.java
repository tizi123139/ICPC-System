package com.scuec.icpc.common.constant;

import java.util.List;

/**
 * 角色编码与层级定义，与接口文档 0.5 节保持一致。
 * 权限层级：指导老师 > 会长 > 部长 > 助理教练 > 干事 > 队长 > 实验室成员 > 正式会员 > 观察会员。
 */
public final class Roles {

    private Roles() {
    }

    public static final String OBSERVE_MEMBER = "OBSERVE_MEMBER";
    public static final String MEMBER = "MEMBER";
    public static final String LAB_MEMBER = "LAB_MEMBER";
    public static final String TEAM_LEADER = "TEAM_LEADER";
    public static final String OFFICER = "OFFICER";
    public static final String ASSISTANT_COACH = "ASSISTANT_COACH";
    public static final String MINISTER = "MINISTER";
    public static final String PRESIDENT = "PRESIDENT";
    public static final String ADVISOR = "ADVISOR";

    public static final List<String> ALL = List.of(
            OBSERVE_MEMBER, MEMBER, LAB_MEMBER, TEAM_LEADER, OFFICER,
            ASSISTANT_COACH, MINISTER, PRESIDENT, ADVISOR);

    /**
     * Spring Security RoleHierarchy 表达式：右侧角色自动继承左侧角色的权限。
     */
    public static final String HIERARCHY = String.join("\n",
            "ROLE_ADVISOR > ROLE_PRESIDENT",
            "ROLE_PRESIDENT > ROLE_MINISTER",
            "ROLE_MINISTER > ROLE_ASSISTANT_COACH",
            "ROLE_ASSISTANT_COACH > ROLE_OFFICER",
            "ROLE_OFFICER > ROLE_TEAM_LEADER",
            "ROLE_TEAM_LEADER > ROLE_LAB_MEMBER",
            "ROLE_LAB_MEMBER > ROLE_MEMBER",
            "ROLE_MEMBER > ROLE_OBSERVE_MEMBER");

    public static String displayName(String code) {
        return switch (code) {
            case OBSERVE_MEMBER -> "观察会员";
            case MEMBER -> "正式会员";
            case LAB_MEMBER -> "竞赛实验室成员";
            case TEAM_LEADER -> "队长";
            case OFFICER -> "干事";
            case ASSISTANT_COACH -> "助理教练";
            case MINISTER -> "部长/副部长";
            case PRESIDENT -> "会长/副会长";
            case ADVISOR -> "指导老师";
            default -> code;
        };
    }
}
