package com.scuec.icpc.module.team.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组队模块的跨表查询结果行。
 */
public final class TeamRows {

    private TeamRows() {
    }

    @Data
    public static class MemberRow {
        private Long teamId;
        private Long memberId;
        private String name;
        private String studentId;
        private String major;
        private String grade;
        private String memberType;
        private String department;
        private LocalDateTime joinTime;
    }

    @Data
    public static class TagScoreRow {
        private Long tagId;
        private String tagName;
        private Integer solvedCount;
    }
}
