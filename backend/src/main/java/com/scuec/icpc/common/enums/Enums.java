package com.scuec.icpc.common.enums;

/**
 * 业务枚举集中定义。数据库中以字符串存储，避免枚举顺序变化导致数据错乱。
 */
public final class Enums {

    private Enums() {
    }

    public enum MemberType {
        OBSERVE_MEMBER, MEMBER, LAB_MEMBER
    }

    public enum MemberStatus {
        ACTIVE, INACTIVE, RESIGNED, REMOVED
    }

    public enum ApplyType {
        JOIN_MEMBER, RESIGN, CAMP_REGISTER, CONTEST_REGISTER
    }

    public enum ApplyStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum AuditAction {
        APPROVE, REJECT
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum Visibility {
        PUBLIC, TRAINING_PRIVATE
    }

    public enum TrainingType {
        SOLO, TEAM
    }

    public enum TrainingStatus {
        DRAFT, PUBLISHED, ONGOING, ENDED
    }

    public enum JudgeResult {
        PENDING, JUDGING, AC, WA, TLE, MLE, RE, CE
    }

    public enum TeamStatus {
        DRAFT, PENDING_CONFIRM, CONFIRMED, DISSOLVED
    }

    public enum CampType {
        WINTER, SUMMER
    }

    public enum CampStatus {
        ANNOUNCED, REGISTERING, ONGOING, ENDED
    }

    public enum AttendanceStatus {
        PRESENT, ABSENT, LEAVE, MAKEUP
    }

    public enum ContestCategory {
        SCHOLASTIC, XCPC, NON_XCPC
    }

    public enum ContestStatus {
        ANNOUNCED, REGISTERING, ONGOING, ENDED
    }

    /**
     * 通用题目关联表的归属类型。
     */
    public enum ProblemTargetType {
        TRAINING, CAMP_TASK, CONTEST
    }
}
