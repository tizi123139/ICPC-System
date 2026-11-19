package com.scuec.icpc.module.recommendation.mapper;

import com.scuec.icpc.module.recommendation.dto.RecommendationRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 推荐模块查询。所有查询都做了无数据兜底，不会因判题未接入而失败。
 */
@Mapper
public interface RecommendationQueryMapper {

    /**
     * 按通过题数升序找薄弱知识点；无提交记录时等价于“题量最多的知识点”。
     */
    @Select("""
            SELECT t.tag_id AS tagId,
                   t.name   AS tagName,
                   COUNT(DISTINCT CASE WHEN s.result = 'AC' THEN s.problem_id END) AS solvedCount,
                   COUNT(DISTINCT pt.problem_id) AS totalCount
            FROM tag t
            JOIN problem_tag pt ON pt.tag_id = t.tag_id
            JOIN problem p ON p.problem_id = pt.problem_id AND p.deleted = 0
            LEFT JOIN submission s
                   ON s.problem_id = p.problem_id
                  AND s.submitter_id = #{memberId}
                  AND s.deleted = 0
            WHERE t.deleted = 0
            GROUP BY t.tag_id, t.name
            ORDER BY solvedCount ASC, totalCount DESC
            LIMIT #{limit}
            """)
    List<RecommendationRows.WeakTagRow> selectWeakTags(@Param("memberId") Long memberId,
                                                       @Param("limit") int limit);

    @Select("""
            <script>
            SELECT p.problem_id     AS problemId,
                   p.title              AS title,
                   p.oj_problem_id      AS ojProblemId,
                   p.difficulty         AS difficulty,
                   MIN(t.name)          AS tagName
            FROM problem p
            JOIN problem_tag pt ON pt.problem_id = p.problem_id
            JOIN tag t ON t.tag_id = pt.tag_id AND t.deleted = 0
            WHERE p.deleted = 0 AND p.visibility = 'PUBLIC'
              AND pt.tag_id IN
            <foreach collection="tagIds" item="id" open="(" separator="," close=")">#{id}</foreach>
              AND p.problem_id NOT IN (
                  SELECT s.problem_id FROM submission s
                  WHERE s.submitter_id = #{memberId} AND s.result = 'AC' AND s.deleted = 0)
            GROUP BY p.problem_id, p.title, p.oj_problem_id, p.difficulty
            ORDER BY MIN(p.create_time) DESC, p.problem_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<RecommendationRows.ProblemRow> selectRecommendedProblems(@Param("memberId") Long memberId,
                                                                  @Param("tagIds") Collection<Long> tagIds,
                                                                  @Param("limit") int limit);

    /**
     * 降级策略：知识点或题目为空时返回最新公开题目。
     */
    @Select("""
            SELECT p.problem_id    AS problemId,
                   p.title         AS title,
                   p.oj_problem_id AS ojProblemId,
                   p.difficulty    AS difficulty,
                   NULL            AS tagName
            FROM problem p
            WHERE p.deleted = 0 AND p.visibility = 'PUBLIC'
            ORDER BY p.create_time DESC, p.problem_id DESC
            LIMIT #{limit}
            """)
    List<RecommendationRows.ProblemRow> selectLatestProblems(@Param("limit") int limit);

    @Select("""
            <script>
            SELECT DISTINCT tr.training_id AS trainingId,
                   tr.name        AS name,
                   tr.type        AS type,
                   tr.start_time  AS startTime,
                   tr.end_time    AS endTime,
                   tr.status      AS status
            FROM training tr
            JOIN problem_link pl
                   ON pl.target_id = tr.training_id AND pl.target_type = 'TRAINING'
            JOIN problem_tag pt ON pt.problem_id = pl.problem_id
            WHERE tr.deleted = 0 AND tr.status != 'DRAFT'
              AND pt.tag_id IN
            <foreach collection="tagIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY tr.start_time DESC, tr.training_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<RecommendationRows.TrainingRow> selectRecommendedTrainings(@Param("tagIds") Collection<Long> tagIds,
                                                                    @Param("limit") int limit);

    @Select("""
            SELECT training_id AS trainingId,
                   name,
                   type,
                   start_time AS startTime,
                   end_time   AS endTime,
                   status
            FROM training
            WHERE deleted = 0 AND status != 'DRAFT'
            ORDER BY start_time DESC, training_id DESC
            LIMIT #{limit}
            """)
    List<RecommendationRows.TrainingRow> selectLatestTrainings(@Param("limit") int limit);
}
