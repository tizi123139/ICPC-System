package com.scuec.icpc.module.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统计模块响应对象。
 */
public final class StatisticsDtos {

    private StatisticsDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountItem {
        private String name;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberOverviewVO {
        private List<CountItem> byGrade;
        private List<CountItem> byMajor;
        private List<CountItem> byDepartment;
        private List<CountItem> byMemberType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopMemberVO {
        private Long memberId;
        private String name;
        private Integer acCount;
        private Integer submitCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingSummaryVO {
        private Long totalTrainings;
        private Long totalSubmissions;
        /**
         * 通过率百分比（0-100，保留一位小数）。判题未接入时恒为 0。
         */
        private Double avgAcRate;
        private List<TopMemberVO> topMembers;
    }
}
