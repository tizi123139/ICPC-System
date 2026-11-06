package com.scuec.icpc.module.contest.dto;

import lombok.Data;

/**
 * 赛事模块自定义 SQL 的扁平结果行。
 */
public final class ContestRows {

    private ContestRows() {
    }

    @Data
    public static class ContestProblemRow {
        private Long contestId;
        private Long problemId;
        private String title;
        private Integer sortOrder;
    }

    @Data
    public static class TeamRow {
        private Long teamId;
        private String name;
        private Long leaderId;
        private String status;
    }

    @Data
    public static class TeamMemberRow {
        private Long teamId;
        private String teamName;
        private Long memberId;
    }
}
