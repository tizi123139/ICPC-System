package com.scuec.icpc.module.team.mapper;

import com.scuec.icpc.module.team.dto.TeamRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 组队模块的跨表查询：成员明细与队伍能力画像。
 */
@Mapper
public interface TeamQueryMapper {

    @Select("""
            <script>
            SELECT tm.team_id     AS teamId,
                   tm.member_id   AS memberId,
                   m.name         AS name,
                   m.student_id   AS studentId,
                   m.major        AS major,
                   m.grade        AS grade,
                   m.member_type  AS memberType,
                   m.department   AS department,
                   tm.join_time   AS joinTime
            FROM team_member tm
            JOIN member m ON m.member_id = tm.member_id AND m.deleted = 0
            WHERE tm.team_id IN
            <foreach collection="teamIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY tm.join_time, tm.id
            </script>
            """)
    List<TeamRows.MemberRow> selectMembersByTeamIds(@Param("teamIds") Collection<Long> teamIds);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT s.problem_id)
            FROM submission s
            WHERE s.deleted = 0
              AND s.result = 'AC'
              AND s.submitter_id IN
            <foreach collection="memberIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    int countSolvedByMembers(@Param("memberIds") Collection<Long> memberIds);

    @Select("""
            <script>
            SELECT t.tag_id AS tagId,
                   t.name   AS tagName,
                   COUNT(DISTINCT s.problem_id) AS solvedCount
            FROM submission s
            JOIN problem_tag pt ON pt.problem_id = s.problem_id
            JOIN tag t ON t.tag_id = pt.tag_id AND t.deleted = 0
            WHERE s.deleted = 0
              AND s.result = 'AC'
              AND s.submitter_id IN
            <foreach collection="memberIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY t.tag_id, t.name
            ORDER BY solvedCount DESC, t.tag_id
            LIMIT #{limit}
            </script>
            """)
    List<TeamRows.TagScoreRow> selectTopTagsByMembers(@Param("memberIds") Collection<Long> memberIds,
                                                      @Param("limit") int limit);
}
