package com.scuec.icpc.module.statistics.dto;

import lombok.Data;

/**
 * 统计模块的查询结果行。
 */
public final class StatisticsRows {

    private StatisticsRows() {
    }

    /**
     * 分组统计结果，name 为分组名，count 为数量。
     */
    @Data
    public static class CountRow {
        private String name;
        private Long count;
    }

    /**
     * 判题结果统计：judged 为已判题数，accepted 为其中通过数。
     */
    @Data
    public static class AcRateRow {
        private Long judged;
        private Long accepted;
    }

    @Data
    public static class TopMemberRow {
        private Long memberId;
        private String name;
        private Integer acCount;
        private Integer submitCount;
    }
}
