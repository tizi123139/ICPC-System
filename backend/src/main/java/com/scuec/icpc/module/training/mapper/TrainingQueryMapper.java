package com.scuec.icpc.module.training.mapper;

import com.scuec.icpc.module.training.dto.TrainingDtos;
import com.scuec.icpc.module.training.dto.TrainingRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 训练模块的跨表查询。题目模块尚未落地，因此这里直接读取 problem / tag 表，
 * 只返回训练页面需要的只读字段，避免与题库模块产生实体耦合。
 */
@Mapper
public interface TrainingQueryMapper {

    @Select("""
            SELECT p.problem_id   AS problemId,
                   p.title        AS title,
                   p.oj_problem_id AS ojProblemId,
                   p.difficulty   AS difficulty,
                   p.time_limit   AS timeLimit,
                   p.memory_limit AS memoryLimit,
                   p.description  AS description,
                   p.input_format AS inputFormat,
                   p.output_format AS outputFormat,
                   p.sample_input AS sampleInput,
                   p.sample_output AS sampleOutput,
                   p.hint         AS hint,
                   p.visibility   AS visibility
            FROM problem_link tp
            JOIN problem p ON p.problem_id = tp.problem_id AND p.deleted = 0
            WHERE tp.target_type = 'TRAINING' AND tp.target_id = #{trainingId}
            ORDER BY tp.sort_order, tp.id
            """)
    List<TrainingDtos.ProblemItem> selectTrainingProblems(@Param("trainingId") Long trainingId);

    @Select("""
            SELECT pt.problem_id AS problemId,
                   t.tag_id      AS tagId,
                   t.name        AS name,
                   t.category    AS category
            FROM problem_link tp
            JOIN problem_tag pt ON pt.problem_id = tp.problem_id
            JOIN tag t ON t.tag_id = pt.tag_id AND t.deleted = 0
            WHERE tp.target_type = 'TRAINING' AND tp.target_id = #{trainingId}
            ORDER BY t.sort_order, t.tag_id
            """)
    List<TrainingRows.ProblemTagRow> selectTrainingProblemTags(@Param("trainingId") Long trainingId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM problem
            WHERE deleted = 0 AND problem_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    long countExistingProblems(@Param("ids") Collection<Long> ids);

    @Select("""
            SELECT t.name AS tagName,
                   COUNT(DISTINCT CASE WHEN s.result = 'AC' THEN s.problem_id END) AS solvedCount,
                   COUNT(DISTINCT s.problem_id) AS attemptCount
            FROM submission s
            JOIN problem_tag pt ON pt.problem_id = s.problem_id
            JOIN tag t ON t.tag_id = pt.tag_id AND t.deleted = 0
            WHERE s.submitter_id = #{memberId} AND s.deleted = 0
            GROUP BY t.tag_id, t.name
            ORDER BY solvedCount DESC, t.name
            """)
    List<TrainingRows.AbilityRow> selectAbilityByMember(@Param("memberId") Long memberId);

    @Select("""
            SELECT COUNT(DISTINCT problem_id)
            FROM submission
            WHERE submitter_id = #{memberId} AND result = 'AC' AND deleted = 0
            """)
    int countSolvedProblems(@Param("memberId") Long memberId);

    @Select("""
            SELECT DATE_FORMAT(s.submit_time, '%Y-%m-%d') AS day,
                   COUNT(DISTINCT CASE WHEN s.result = 'AC' THEN s.problem_id END) AS acCount,
                   COUNT(*) AS submitCount
            FROM submission s
            WHERE s.submitter_id = #{memberId}
              AND s.deleted = 0
              AND s.submit_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            GROUP BY day
            ORDER BY day
            """)
    List<TrainingRows.TrendRow> selectRecentTrend(@Param("memberId") Long memberId);

    @Select("""
            <script>
            SELECT tm.member_id AS memberId,
                   t.team_id   AS teamId,
                   t.name      AS teamName
            FROM team_member tm
            JOIN team t ON t.team_id = tm.team_id AND t.deleted = 0
            WHERE tm.member_id IN
            <foreach collection="memberIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<TrainingRows.TeamMemberRow> selectTeamsByMemberIds(@Param("memberIds") Collection<Long> memberIds);
}
