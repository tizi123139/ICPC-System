package com.scuec.icpc.module.team.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.enums.Enums;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.team.dto.TeamDtos;
import com.scuec.icpc.module.team.dto.TeamRows;
import com.scuec.icpc.module.team.entity.Team;
import com.scuec.icpc.module.team.entity.TeamMember;
import com.scuec.icpc.module.team.mapper.TeamMapper;
import com.scuec.icpc.module.team.mapper.TeamMemberMapper;
import com.scuec.icpc.module.team.mapper.TeamQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 组队业务：队伍增查改、成员邀请/移除、教练确认。
 * 状态流转：PENDING_CONFIRM → CONFIRMED（确认后成员锁定）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    /**
     * 与 contest.max_team_size 的默认值保持一致。
     */
    private static final int MAX_TEAM_SIZE = 3;

    private static final int TOP_TAG_LIMIT = 5;

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamQueryMapper queryMapper;
    private final MemberMapper memberMapper;

    // ==================== 查询 ====================

    public PageResult<TeamDtos.TeamVO> page(TeamDtos.Query query) {
        LambdaQueryWrapper<Team> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Team::getStatus, parseEnum(Enums.TeamStatus.class,
                    query.getStatus(), "status").name());
        }
        // team 表暂无 contest_id 字段，contestId 参数暂不参与过滤，待表结构补充后接入
        wrapper.orderByDesc(Team::getCreateTime).orderByDesc(Team::getTeamId);

        IPage<Team> page = teamMapper.selectPage(query.toPage(), wrapper);
        Map<Long, List<TeamRows.MemberRow>> memberMap = loadMemberMap(
                page.getRecords().stream().map(Team::getTeamId).toList());
        return PageResult.of(page, team -> toVO(team, memberMap));
    }

    public TeamDtos.TeamDetailVO detail(Long teamId) {
        Team team = getOrThrow(teamId);
        List<TeamRows.MemberRow> rows = queryMapper.selectMembersByTeamIds(List.of(teamId));
        List<Long> memberIds = rows.stream().map(TeamRows.MemberRow::getMemberId).toList();
        return TeamDtos.TeamDetailVO.builder()
                .basicInfo(toVO(team, Map.of(teamId, rows)))
                .members(rows.stream().map(this::toMemberVO).toList())
                .abilitySummary(buildAbility(memberIds))
                .build();
    }

    // ==================== 写操作 ====================

    @Transactional
    public Long create(TeamDtos.CreateRequest request) {
        Long leaderId = SecurityUtils.currentMemberId();
        requireActiveMember(leaderId);

        List<Long> invited = request.getInvitedMemberIds() == null
                ? List.of()
                : request.getInvitedMemberIds().stream()
                        .filter(Objects::nonNull).distinct().toList();
        if (invited.contains(leaderId)) {
            throw BusinessException.badRequest("邀请列表不能包含创建者本人");
        }
        if (invited.size() + 1 > MAX_TEAM_SIZE) {
            throw BusinessException.badRequest("队伍最多 " + MAX_TEAM_SIZE + " 人");
        }
        invited.forEach(this::requireActiveMember);

        Team team = new Team();
        team.setName(request.getName().trim());
        team.setLeaderId(leaderId);
        team.setContestTarget(request.getContestTarget().trim());
        team.setDescription(request.getDescription());
        team.setStatus(Enums.TeamStatus.PENDING_CONFIRM.name());
        teamMapper.insert(team);

        addMember(team.getTeamId(), leaderId);
        invited.forEach(memberId -> addMember(team.getTeamId(), memberId));

        log.info("会员 {} 创建队伍 {}（{}），初始成员 {} 人",
                leaderId, team.getTeamId(), team.getName(), invited.size() + 1);
        return team.getTeamId();
    }

    @Transactional
    public void update(Long teamId, TeamDtos.UpdateRequest request) {
        Team team = getOrThrow(teamId);
        Long callerId = SecurityUtils.currentMemberId();
        boolean manager = SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH);
        if (!team.getLeaderId().equals(callerId) && !manager) {
            throw BusinessException.forbidden("仅队长或教练可修改队伍信息");
        }
        if (!manager) {
            requireEditable(team, "修改队伍信息");
        }

        Long leaderId = team.getLeaderId();
        if (request.getLeaderId() != null && !request.getLeaderId().equals(leaderId)) {
            requireMemberInTeam(teamId, request.getLeaderId());
            leaderId = request.getLeaderId();
        }

        // updateById 会忽略 null 字段，这里统一用 UpdateWrapper 显式赋值
        teamMapper.update(null, Wrappers.<Team>lambdaUpdate()
                .set(StringUtils.hasText(request.getName()),
                        Team::getName, request.getName() == null ? null : request.getName().trim())
                .set(Team::getLeaderId, leaderId)
                .set(StringUtils.hasText(request.getContestTarget()),
                        Team::getContestTarget,
                        request.getContestTarget() == null ? null : request.getContestTarget().trim())
                .set(request.getDescription() != null, Team::getDescription, request.getDescription())
                .set(Team::getUpdateTime, LocalDateTime.now())
                .eq(Team::getTeamId, teamId));

        if (!leaderId.equals(team.getLeaderId())) {
            log.info("队伍 {} 队长由 {} 转让给 {}", teamId, team.getLeaderId(), leaderId);
        }
    }

    @Transactional
    public void invite(Long teamId, TeamDtos.MemberRequest request) {
        Team team = getOrThrow(teamId);
        requireLeaderOrManager(team, "邀请成员");
        requireEditable(team, "邀请成员");

        Long memberId = request.getMemberId();
        if (countMembers(teamId) >= MAX_TEAM_SIZE) {
            throw BusinessException.conflict("队伍人数已达上限 " + MAX_TEAM_SIZE);
        }
        Long exists = teamMemberMapper.selectCount(Wrappers.<TeamMember>lambdaQuery()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getMemberId, memberId));
        if (exists != null && exists > 0) {
            throw BusinessException.conflict("该成员已在队伍中");
        }
        requireActiveMember(memberId);

        addMember(teamId, memberId);
        log.info("队伍 {} 邀请成员 {}", teamId, memberId);
    }

    @Transactional
    public void removeMember(Long teamId, Long memberId) {
        Team team = getOrThrow(teamId);
        Long callerId = SecurityUtils.currentMemberId();
        boolean manager = SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH);
        boolean self = callerId.equals(memberId);
        if (!self && !team.getLeaderId().equals(callerId) && !manager) {
            throw BusinessException.forbidden("仅队长、本人或教练可移除成员");
        }
        requireEditable(team, "移除成员");

        if (team.getLeaderId().equals(memberId)) {
            throw BusinessException.conflict("队长不能被移除，请先转让队长");
        }
        int removed = teamMemberMapper.delete(Wrappers.<TeamMember>lambdaQuery()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getMemberId, memberId));
        if (removed == 0) {
            throw BusinessException.notFound("该成员不在队伍中");
        }
        log.info("队伍 {} 移除成员 {}", teamId, memberId);
    }

    @Transactional
    public void confirm(Long teamId) {
        Team team = getOrThrow(teamId);
        if (Enums.TeamStatus.CONFIRMED.name().equals(team.getStatus())) {
            throw BusinessException.conflict("队伍已确认，无需重复操作");
        }
        if (Enums.TeamStatus.DISSOLVED.name().equals(team.getStatus())) {
            throw BusinessException.conflict("队伍已解散，无法确认");
        }
        if (countMembers(teamId) == 0) {
            throw BusinessException.badRequest("队伍没有成员，无法确认");
        }

        teamMapper.update(null, Wrappers.<Team>lambdaUpdate()
                .set(Team::getStatus, Enums.TeamStatus.CONFIRMED.name())
                .set(Team::getUpdateTime, LocalDateTime.now())
                .eq(Team::getTeamId, teamId));
        log.info("队伍 {} 已由会员 {} 确认", teamId, SecurityUtils.currentMemberId());
    }

    // ==================== 内部工具 ====================

    private Team getOrThrow(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("队伍不存在");
        }
        return team;
    }

    private void requireLeaderOrManager(Team team, String action) {
        if (!team.getLeaderId().equals(SecurityUtils.currentMemberId())
                && !SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH)) {
            throw BusinessException.forbidden("仅队长或教练可" + action);
        }
    }

    private void requireEditable(Team team, String action) {
        if (Enums.TeamStatus.CONFIRMED.name().equals(team.getStatus())) {
            throw BusinessException.conflict("队伍已确认，不能" + action);
        }
        if (Enums.TeamStatus.DISSOLVED.name().equals(team.getStatus())) {
            throw BusinessException.conflict("队伍已解散，不能" + action);
        }
    }

    private void requireMemberInTeam(Long teamId, Long memberId) {
        Long exists = teamMemberMapper.selectCount(Wrappers.<TeamMember>lambdaQuery()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getMemberId, memberId));
        if (exists == null || exists == 0) {
            throw BusinessException.badRequest("新队长必须是队伍成员");
        }
    }

    private void requireActiveMember(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.notFound("会员不存在：" + memberId);
        }
        if (!Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.conflict("会员 " + member.getName() + " 状态异常，无法加入队伍");
        }
    }

    private int countMembers(Long teamId) {
        Long count = teamMemberMapper.selectCount(Wrappers.<TeamMember>lambdaQuery()
                .eq(TeamMember::getTeamId, teamId));
        return count == null ? 0 : count.intValue();
    }

    private void addMember(Long teamId, Long memberId) {
        TeamMember teamMember = new TeamMember();
        teamMember.setTeamId(teamId);
        teamMember.setMemberId(memberId);
        teamMember.setJoinTime(LocalDateTime.now());
        teamMemberMapper.insert(teamMember);
    }

    private Map<Long, List<TeamRows.MemberRow>> loadMemberMap(Collection<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return queryMapper.selectMembersByTeamIds(teamIds).stream()
                .collect(Collectors.groupingBy(TeamRows.MemberRow::getTeamId));
    }

    private TeamDtos.TeamVO toVO(Team team, Map<Long, List<TeamRows.MemberRow>> memberMap) {
        List<Long> memberIds = memberMap.getOrDefault(team.getTeamId(), List.of()).stream()
                .map(TeamRows.MemberRow::getMemberId)
                .toList();
        return TeamDtos.TeamVO.builder()
                .teamId(team.getTeamId())
                .name(team.getName())
                .leaderId(team.getLeaderId())
                .memberIds(memberIds)
                .contestTarget(team.getContestTarget())
                .description(team.getDescription())
                .status(team.getStatus())
                .createTime(team.getCreateTime())
                .build();
    }

    private TeamDtos.TeamMemberVO toMemberVO(TeamRows.MemberRow row) {
        return TeamDtos.TeamMemberVO.builder()
                .memberId(row.getMemberId())
                .name(row.getName())
                .studentId(row.getStudentId())
                .major(row.getMajor())
                .grade(row.getGrade())
                .memberType(row.getMemberType())
                .department(row.getDepartment())
                .joinTime(row.getJoinTime())
                .build();
    }

    private TeamDtos.AbilitySummaryVO buildAbility(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return TeamDtos.AbilitySummaryVO.builder()
                    .memberCount(0).totalSolved(0).topTags(List.of()).build();
        }
        int totalSolved = queryMapper.countSolvedByMembers(memberIds);
        List<TeamDtos.TagScoreVO> topTags = queryMapper
                .selectTopTagsByMembers(memberIds, TOP_TAG_LIMIT).stream()
                .map(row -> TeamDtos.TagScoreVO.builder()
                        .tagId(row.getTagId())
                        .tagName(row.getTagName())
                        .solvedCount(row.getSolvedCount() == null ? 0 : row.getSolvedCount())
                        .build())
                .toList();
        return TeamDtos.AbilitySummaryVO.builder()
                .memberCount(memberIds.size())
                .totalSolved(totalSolved)
                .topTags(topTags)
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw BusinessException.badRequest(field + " 取值不合法：" + value);
        }
    }
}
