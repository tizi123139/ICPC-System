package com.scuec.icpc.module.statistics.mapper;

import com.scuec.icpc.module.statistics.dto.StatisticsRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计模块的聚合查询。日期区间均为闭区间，包含 endDate 当天。
 */
@Mapper
public interface StatisticsQueryMapper {

    @Select("""
            SELECT COALESCE(grade, '未填写') AS name, COUNT(*) AS count
            FROM member
            WHERE deleted = 0
            GROUP BY COALESCE(grade, '未填写')
            ORDER BY count DESC
            """)
    List<StatisticsRows.CountRow> countByGrade();

    @Select("""
            SELECT COALESCE(major, '未填写') AS name, COUNT(*) AS count
            FROM member
            WHERE deleted = 0
            GROUP BY COALESCE(major, '未填写')
            ORDER BY count DESC
            """)
    List<StatisticsRows.CountRow> countByMajor();

    @Select("""
            SELECT COALESCE(department, '未填写') AS name, COUNT(*) AS count
            FROM member
            WHERE deleted = 0
            GROUP BY COALESCE(department, '未填写')
            ORDER BY count DESC
            """)
    List<StatisticsRows.CountRow> countByDepartment();

    @Select("""
            SELECT COALESCE(member_type, '未填写') AS name, COUNT(*) AS count
            FROM member
            WHERE deleted = 0
            GROUP BY COALESCE(member_type, '未填写')
            ORDER BY count DESC
            """)
    List<StatisticsRows.CountRow> countByMemberType();

    @Select("""
            <script>
            SELECT COUNT(*) FROM training
            WHERE deleted = 0
            <if test="startDate != null">AND create_time &gt;= #{startDate}</if>
            <if test="endDate != null">AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>
            </script>
            """)
    long countTrainings(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

    @Select("""
            <script>
            SELECT COUNT(*) FROM submission
            WHERE deleted = 0
            <if test="startDate != null">AND submit_time &gt;= #{startDate}</if>
            <if test="endDate != null">AND submit_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>
            </script>
            """)
    long countSubmissions(@Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    /**
     * 只统计已判题的提交，避免 PENDING 拉低通过率。
     */
    @Select("""
            <script>
            SELECT COUNT(*) AS judged,
                   SUM(CASE WHEN result = 'AC' THEN 1 ELSE 0 END) AS accepted
            FROM submission
            WHERE deleted = 0 AND result &lt;&gt; 'PENDING'
            <if test="startDate != null">AND submit_time &gt;= #{startDate}</if>
            <if test="endDate != null">AND submit_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>
            </script>
            """)
    StatisticsRows.AcRateRow countAcRate(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Select("""
            <script>
            SELECT s.submitter_id AS memberId,
                   m.name         AS name,
                   COUNT(DISTINCT CASE WHEN s.result = 'AC' THEN s.problem_id END) AS acCount,
                   COUNT(*)       AS submitCount
            FROM submission s
            JOIN member m ON m.member_id = s.submitter_id AND m.deleted = 0
            WHERE s.deleted = 0
            <if test="startDate != null">AND s.submit_time &gt;= #{startDate}</if>
            <if test="endDate != null">AND s.submit_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>
            GROUP BY s.submitter_id, m.name
            ORDER BY acCount DESC, submitCount DESC
            LIMIT #{limit}
            </script>
            """)
    List<StatisticsRows.TopMemberRow> selectTopMembers(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("limit") int limit);

    /**
     * 按日聚合提交数与 AC 数，供训练提交趋势折线图使用。
     */
    @Select("""
            <script>
            SELECT DATE_FORMAT(submit_time, '%Y-%m-%d') AS date,
                   COUNT(*) AS submissions,
                   SUM(CASE WHEN result = 'AC' THEN 1 ELSE 0 END) AS ac
            FROM submission
            WHERE deleted = 0 AND submit_time IS NOT NULL
            <if test="startDate != null">AND submit_time &gt;= #{startDate}</if>
            <if test="endDate != null">AND submit_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>
            GROUP BY DATE_FORMAT(submit_time, '%Y-%m-%d')
            ORDER BY date
            </script>
            """)
    List<StatisticsRows.TrendRow> selectSubmissionTrend(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
}
