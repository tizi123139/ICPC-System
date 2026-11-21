package com.scuec.icpc.module.statistics.service;

import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.module.statistics.dto.StatisticsDtos;
import com.scuec.icpc.module.statistics.dto.StatisticsRows;
import com.scuec.icpc.module.statistics.mapper.StatisticsQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据统计业务：会员结构与训练汇总。
 * 判题未接入时 submission 全是 PENDING，因此通过率按“已判题提交”计算，无数据时为 0。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final int TOP_MEMBER_LIMIT = 10;

    private final StatisticsQueryMapper queryMapper;

    @Cacheable(cacheNames = "statisticsMembers", key = "'overview'")
    public StatisticsDtos.MemberOverviewVO membersOverview() {
        return StatisticsDtos.MemberOverviewVO.builder()
                .byGrade(toCountItems(queryMapper.countByGrade()))
                .byMajor(toCountItems(queryMapper.countByMajor()))
                .byDepartment(toCountItems(queryMapper.countByDepartment()))
                .byMemberType(toCountItems(queryMapper.countByMemberType()))
                .build();
    }

    @Cacheable(cacheNames = "statisticsTraining",
            key = "T(java.util.Objects).toString(#startDate, 'all') + ':' "
                    + "+ T(java.util.Objects).toString(#endDate, 'all')")
    public StatisticsDtos.TrainingSummaryVO trainingSummary(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw BusinessException.badRequest("开始日期不能晚于结束日期");
        }

        long totalTrainings = queryMapper.countTrainings(startDate, endDate);
        long totalSubmissions = queryMapper.countSubmissions(startDate, endDate);

        StatisticsRows.AcRateRow rate = queryMapper.countAcRate(startDate, endDate);
        double avgAcRate = 0.0;
        if (rate != null && rate.getJudged() != null && rate.getJudged() > 0) {
            long accepted = rate.getAccepted() == null ? 0L : rate.getAccepted();
            avgAcRate = Math.round(accepted * 1000.0 / rate.getJudged()) / 10.0;
        }

        List<StatisticsDtos.TopMemberVO> topMembers = queryMapper
                .selectTopMembers(startDate, endDate, TOP_MEMBER_LIMIT).stream()
                .map(row -> StatisticsDtos.TopMemberVO.builder()
                        .memberId(row.getMemberId())
                        .name(row.getName())
                        .acCount(row.getAcCount() == null ? 0 : row.getAcCount())
                        .submitCount(row.getSubmitCount() == null ? 0 : row.getSubmitCount())
                        .build())
                .toList();

        return StatisticsDtos.TrainingSummaryVO.builder()
                .totalTrainings(totalTrainings)
                .totalSubmissions(totalSubmissions)
                .avgAcRate(avgAcRate)
                .topMembers(topMembers)
                .trend(queryMapper.selectSubmissionTrend(startDate, endDate).stream()
                        .map(row -> StatisticsDtos.TrendItemVO.builder()
                                .date(row.getDate())
                                .submissions(row.getSubmissions() == null ? 0 : row.getSubmissions())
                                .ac(row.getAc() == null ? 0 : row.getAc())
                                .build())
                        .toList())
                .build();
    }

    private List<StatisticsDtos.CountItem> toCountItems(List<StatisticsRows.CountRow> rows) {
        return rows.stream()
                .map(row -> StatisticsDtos.CountItem.builder()
                        .name(row.getName())
                        .count(row.getCount() == null ? 0L : row.getCount())
                        .build())
                .toList();
    }
}
