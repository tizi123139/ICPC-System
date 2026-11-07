package com.scuec.icpc.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scuec.icpc.module.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 会员档案中的训练记录概览（按训练聚合）。
     */
    @Select("""
            SELECT t.training_id             AS trainingId,
                   t.name                    AS trainingName,
                   COUNT(DISTINCT CASE WHEN s.result = 'AC' THEN s.problem_id END) AS solvedCount,
                   COUNT(*)                  AS submitCount,
                   MAX(s.submit_time)        AS lastSubmitTime
            FROM submission s
            JOIN training t ON t.training_id = s.training_id AND t.deleted = 0
            WHERE s.submitter_id = #{memberId} AND s.deleted = 0
            GROUP BY t.training_id, t.name
            ORDER BY lastSubmitTime DESC
            LIMIT 100
            """)
    List<Map<String, Object>> selectTrainingRecords(@Param("memberId") Long memberId);

    /**
     * 会员档案中的赛事报名与成绩记录。
     */
    @Select("""
            SELECT c.contest_id        AS contestId,
                   c.name              AS contestName,
                   c.category          AS category,
                   cr.status           AS registrationStatus,
                   cr.audit_remark     AS auditRemark,
                   cres.rank_no        AS rankNo,
                   cres.solved_count   AS solvedCount,
                   cres.penalty        AS penalty,
                   cres.award          AS award
            FROM application cr
            JOIN contest c ON c.contest_id = cr.target_id AND c.deleted = 0
            LEFT JOIN contest_result cres
                   ON cres.contest_id = cr.target_id
                  AND cres.member_id = cr.applicant_id
                  AND cres.deleted = 0
            WHERE cr.applicant_id = #{memberId}
              AND cr.type = 'CONTEST_REGISTER'
              AND cr.deleted = 0
            ORDER BY c.start_time DESC
            LIMIT 100
            """)
    List<Map<String, Object>> selectContestRecords(@Param("memberId") Long memberId);
}
