package com.scuec.icpc.module.contest.mapper;

import com.scuec.icpc.module.contest.dto.ContestRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 赛事模块的跨表查询。题目、队伍模块尚未落地，这里只读取必要字段。
 */
@Mapper
public interface ContestQueryMapper {

    @Select("""
            <script>
            SELECT cp.target_id  AS contestId,
                   cp.problem_id AS problemId,
                   p.title       AS title,
                   cp.sort_order AS sortOrder
            FROM problem_link cp
            JOIN problem p ON p.problem_id = cp.problem_id AND p.deleted = 0
            WHERE cp.target_type = 'CONTEST' AND cp.target_id IN
            <foreach collection="contestIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY cp.sort_order, cp.id
            </script>
            """)
    List<ContestRows.ContestProblemRow> selectContestProblems(
            @Param("contestIds") Collection<Long> contestIds);

    @Select("""
            <script>
            SELECT COUNT(*) FROM problem
            WHERE deleted = 0 AND problem_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    long countExistingProblems(@Param("ids") Collection<Long> ids);

    @Select("""
            SELECT team_id AS teamId, name, leader_id AS leaderId, status
            FROM team
            WHERE team_id = #{teamId} AND deleted = 0
            """)
    ContestRows.TeamRow selectTeam(@Param("teamId") Long teamId);

    @Select("SELECT COUNT(*) FROM team_member WHERE team_id = #{teamId}")
    long countTeamMembers(@Param("teamId") Long teamId);

    @Select("""
            <script>
            SELECT team_id AS teamId, name, leader_id AS leaderId, status
            FROM team
            WHERE deleted = 0 AND team_id IN
            <foreach collection="teamIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<ContestRows.TeamRow> selectTeams(@Param("teamIds") Collection<Long> teamIds);

    @Select("""
            SELECT tm.team_id AS teamId, t.name AS teamName, tm.member_id AS memberId
            FROM team_member tm
            JOIN team t ON t.team_id = tm.team_id AND t.deleted = 0
            WHERE tm.member_id = #{memberId}
            """)
    List<ContestRows.TeamMemberRow> selectTeamsByMember(@Param("memberId") Long memberId);

    @Select("""
            SELECT COUNT(*) FROM team_member
            WHERE team_id = #{teamId} AND member_id = #{memberId}
            """)
    long countTeamMember(@Param("teamId") Long teamId, @Param("memberId") Long memberId);

    @Select("""
            <script>
            SELECT tm.team_id AS teamId, t.name AS teamName, tm.member_id AS memberId
            FROM team_member tm
            JOIN team t ON t.team_id = tm.team_id AND t.deleted = 0
            WHERE tm.team_id IN
            <foreach collection="teamIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<ContestRows.TeamMemberRow> selectTeamMembers(
            @Param("teamIds") Collection<Long> teamIds);
}
