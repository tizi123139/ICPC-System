package com.scuec.icpc.module.contest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.enums.Enums;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.contest.dto.ContestDtos;
import com.scuec.icpc.module.contest.dto.ContestRows;
import com.scuec.icpc.module.contest.entity.Contest;
import com.scuec.icpc.module.contest.entity.ContestProblem;
import com.scuec.icpc.module.contest.entity.ContestRegistration;
import com.scuec.icpc.module.contest.entity.ContestResult;
import com.scuec.icpc.module.contest.mapper.ContestMapper;
import com.scuec.icpc.module.contest.mapper.ContestProblemMapper;
import com.scuec.icpc.module.contest.mapper.ContestQueryMapper;
import com.scuec.icpc.module.contest.mapper.ContestRegistrationMapper;
import com.scuec.icpc.module.contest.mapper.ContestResultMapper;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.training.entity.Submission;
import com.scuec.icpc.module.training.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 赛事管理业务：赛事发布、报名审核、实时榜单、赛中提交、成绩归档与证书统计。
 * 校赛 / XCPC / 非 XCPC 共用同一套接口，通过 category 区分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestService {

    private static final Set<String> WRONG_RESULTS = Set.of("WA", "TLE", "MLE", "RE", "CE");

    private final ContestMapper contestMapper;
    private final ContestRegistrationMapper registrationMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestResultMapper resultMapper;
    private final ContestQueryMapper queryMapper;
    private final SubmissionMapper submissionMapper;
    private final MemberMapper memberMapper;

    // ==================== 赛事信息 ====================

    public PageResult<ContestDtos.ContestVO> list(ContestDtos.Query query) {
        LambdaQueryWrapper<Contest> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(Contest::getCategory,
                    parseEnum(Enums.ContestCategory.class, query.getCategory(), "category").name());
        }
        wrapper.orderByDesc(Contest::getStartTime).orderByDesc(Contest::getContestId);

        IPage<Contest> page = contestMapper.selectPage(query.toPage(), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }
        List<Long> contestIds = page.getRecords().stream()
                .map(Contest::getContestId).toList();
        Map<Long, List<ContestRows.ContestProblemRow>> problemMap =
                loadContestProblems(contestIds);
        Map<Long, Member> creatorMap = loadMembers(page.getRecords().stream()
                .map(Contest::getCreatorId).toList());
        LocalDateTime now = LocalDateTime.now();
        List<ContestDtos.ContestVO> records = page.getRecords().stream()
                .map(contest -> toVO(contest, now, creatorMap.get(contest.getCreatorId()),
                        problemMap.getOrDefault(contest.getContestId(), List.of())))
                .toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Transactional
    public ContestDtos.ContestVO create(ContestDtos.CreateRequest request) {
        String category = parseEnum(Enums.ContestCategory.class,
                request.getCategory(), "category").name();
        validateContestTime(request.getStartTime(), request.getEndTime(),
                request.getRegistrationDeadline());

        List<Long> problemIds = distinct(request.getProblemIds());
        validateProblems(problemIds);

        Contest contest = new Contest();
        contest.setName(request.getName().trim());
        contest.setCategory(category);
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        contest.setRegistrationDeadline(request.getRegistrationDeadline());
        contest.setDescription(request.getDescription());
        contest.setRequirements(request.getRequirements());
        contest.setMaxTeamSize(request.getMaxTeamSize() == null ? 3 : request.getMaxTeamSize());
        contest.setFreezeMinutes(request.getFreezeMinutes() == null ? 60 : request.getFreezeMinutes());
        contest.setStatus(Enums.ContestStatus.ANNOUNCED.name());
        contest.setCreatorId(SecurityUtils.currentMemberId());
        contestMapper.insert(contest);
        saveContestProblems(contest.getContestId(), problemIds);

        log.info("创建赛事 {}（{}），类别 {}", contest.getContestId(), contest.getName(), category);
        return toVO(contest, LocalDateTime.now(),
                memberMapper.selectById(contest.getCreatorId()),
                loadContestProblems(List.of(contest.getContestId()))
                        .getOrDefault(contest.getContestId(), List.of()));
    }

    public ContestDtos.ContestVO detail(Long contestId) {
        Contest contest = getOrThrow(contestId);
        Member creator = contest.getCreatorId() == null
                ? null : memberMapper.selectById(contest.getCreatorId());
        return toVO(contest, LocalDateTime.now(), creator,
                loadContestProblems(List.of(contestId))
                        .getOrDefault(contestId, List.of()));
    }

    // ==================== 报名与审核 ====================

    @Transactional
    public void register(Long contestId, ContestDtos.RegisterRequest request) {
        Contest contest = getOrThrow(contestId);
        Long memberId = SecurityUtils.currentMemberId();
        Member member = memberMapper.selectById(memberId);
        if (member == null || !Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.forbidden("当前会员状态不允许报名赛事");
        }

        LocalDateTime now = LocalDateTime.now();
        if (contest.getRegistrationDeadline() != null
                && now.isAfter(contest.getRegistrationDeadline())) {
            throw BusinessException.conflict("赛事报名已截止");
        }
        if (contest.getStartTime() != null && !now.isBefore(contest.getStartTime())) {
            throw BusinessException.conflict("赛事已开始，无法报名");
        }

        if (request.getTeamId() != null) {
            ContestRows.TeamRow team = queryMapper.selectTeam(request.getTeamId());
            if (team == null) {
                throw BusinessException.notFound("队伍不存在");
            }
            if (!memberId.equals(team.getLeaderId())) {
                throw BusinessException.forbidden("仅队长可代表队伍报名");
            }
            if (queryMapper.countTeamMembers(team.getTeamId()) > contest.getMaxTeamSize()) {
                throw BusinessException.conflict("队伍人数超过赛事上限");
            }
            Long duplicated = registrationMapper.selectCount(
                    Wrappers.<ContestRegistration>lambdaQuery()
                            .eq(ContestRegistration::getContestId, contestId)
                            .eq(ContestRegistration::getTeamId, team.getTeamId())
                            .in(ContestRegistration::getStatus,
                                    Enums.ApplyStatus.PENDING.name(),
                                    Enums.ApplyStatus.APPROVED.name()));
            if (duplicated != null && duplicated > 0) {
                throw BusinessException.conflict("该队伍已报名本赛事，请勿重复提交");
            }
        } else {
            Long duplicated = registrationMapper.selectCount(
                    Wrappers.<ContestRegistration>lambdaQuery()
                            .eq(ContestRegistration::getContestId, contestId)
                            .eq(ContestRegistration::getMemberId, memberId)
                            .isNull(ContestRegistration::getTeamId)
                            .in(ContestRegistration::getStatus,
                                    Enums.ApplyStatus.PENDING.name(),
                                    Enums.ApplyStatus.APPROVED.name()));
            if (duplicated != null && duplicated > 0) {
                throw BusinessException.conflict("已提交过本赛事报名，请勿重复提交");
            }
            List<Long> myTeamIds = queryMapper.selectTeamsByMember(memberId).stream()
                    .map(ContestRows.TeamMemberRow::getTeamId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!myTeamIds.isEmpty()) {
                Long teamRegistrations = registrationMapper.selectCount(
                        Wrappers.<ContestRegistration>lambdaQuery()
                                .eq(ContestRegistration::getContestId, contestId)
                                .in(ContestRegistration::getTeamId, myTeamIds)
                                .in(ContestRegistration::getStatus,
                                        Enums.ApplyStatus.PENDING.name(),
                                        Enums.ApplyStatus.APPROVED.name()));
                if (teamRegistrations != null && teamRegistrations > 0) {
                    throw BusinessException.conflict("所在队伍已报名本赛事，无需个人重复报名");
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("materials", request.getMaterials() == null ? List.of() : request.getMaterials());
        data.put("remark", request.getRemark());

        ContestRegistration registration = new ContestRegistration();
        registration.setContestId(contestId);
        registration.setTeamId(request.getTeamId());
        registration.setMemberId(memberId);
        registration.setApplyData(data);
        registration.setStatus(Enums.ApplyStatus.PENDING.name());
        registration.setApplyTime(now);
        registrationMapper.insert(registration);

        log.info("会员 {} 报名赛事 {}，队伍 {}，报名记录 {}",
                memberId, contestId, request.getTeamId(),
                registration.getRegistrationId());
    }

    public List<ContestDtos.RegistrationVO> registrations(Long contestId,
                                                          ContestDtos.RegistrationQuery query) {
        getOrThrow(contestId);
        LambdaQueryWrapper<ContestRegistration> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ContestRegistration::getContestId, contestId);
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ContestRegistration::getStatus,
                    parseEnum(Enums.ApplyStatus.class, query.getStatus(), "status").name());
        }
        wrapper.orderByDesc(ContestRegistration::getApplyTime)
                .orderByDesc(ContestRegistration::getRegistrationId)
                .last("LIMIT 500");

        List<ContestRegistration> registrations = registrationMapper.selectList(wrapper);
        if (registrations.isEmpty()) {
            return List.of();
        }
        List<Long> memberIds = new ArrayList<>();
        List<Long> teamIds = new ArrayList<>();
        registrations.forEach(registration -> {
            memberIds.add(registration.getMemberId());
            if (registration.getAuditorId() != null) {
                memberIds.add(registration.getAuditorId());
            }
            if (registration.getTeamId() != null) {
                teamIds.add(registration.getTeamId());
            }
        });
        Map<Long, Member> memberMap = loadMembers(memberIds);
        Map<Long, ContestRows.TeamRow> teamMap = loadTeams(teamIds);
        return registrations.stream()
                .map(registration -> toRegistrationVO(registration, memberMap, teamMap))
                .toList();
    }

    @Transactional
    public void audit(Long contestId, Long applyId, ContestDtos.AuditRequest request) {
        getOrThrow(contestId);
        ContestRegistration registration = registrationMapper.selectById(applyId);
        if (registration == null || !contestId.equals(registration.getContestId())) {
            throw BusinessException.notFound("赛事报名记录不存在");
        }
        if (!Enums.ApplyStatus.PENDING.name().equals(registration.getStatus())) {
            throw BusinessException.conflict("该报名已处理，不能重复审批");
        }
        Enums.AuditAction action = parseEnum(Enums.AuditAction.class,
                request.getAction(), "action");

        registration.setStatus(action == Enums.AuditAction.APPROVE
                ? Enums.ApplyStatus.APPROVED.name()
                : Enums.ApplyStatus.REJECTED.name());
        registration.setAuditRemark(request.getRemark());
        registration.setAuditorId(SecurityUtils.currentMemberId());
        registration.setAuditTime(LocalDateTime.now());
        registrationMapper.updateById(registration);

        log.info("赛事 {} 报名 {} 审批结果：{}",
                contestId, applyId, registration.getStatus());
    }

    // ==================== 赛中提交 ====================

    @Transactional
    public ContestDtos.SubmitVO submit(Long contestId, ContestDtos.SubmitRequest request) {
        Contest contest = getOrThrow(contestId);
        LocalDateTime now = LocalDateTime.now();
        if (contest.getStartTime() == null || contest.getEndTime() == null
                || now.isBefore(contest.getStartTime()) || !now.isBefore(contest.getEndTime())) {
            throw BusinessException.conflict("当前不在赛事开放时段内");
        }

        Long memberId = SecurityUtils.currentMemberId();
        Member member = memberMapper.selectById(memberId);
        if (member == null || !Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.forbidden("当前会员状态不允许提交代码");
        }

        long problemCount = contestProblemMapper.selectCount(
                Wrappers.<ContestProblem>lambdaQuery()
                        .eq(ContestProblem::getContestId, contestId)
                        .eq(ContestProblem::getProblemId, request.getProblemId()));
        if (problemCount == 0) {
            throw BusinessException.badRequest("该题目不属于本次赛事");
        }
        if (!isApprovedParticipant(contestId, memberId)) {
            throw BusinessException.forbidden("仅报名审核通过的选手可提交代码");
        }

        Submission submission = new Submission();
        submission.setContestId(contestId);
        submission.setProblemId(request.getProblemId());
        submission.setSubmitterId(memberId);
        submission.setCode(request.getCode());
        submission.setLanguage(request.getLanguage().trim());
        submission.setResult(Enums.JudgeResult.PENDING.name());
        submission.setSubmitTime(now);
        submissionMapper.insert(submission);

        // TODO 接入 OJ 判题：投递判题队列，异步回写 result/timeUsed/memoryUsed。
        log.info("会员 {} 提交赛事 {} 题目 {}，提交记录 {}",
                memberId, contestId, request.getProblemId(), submission.getSubmitId());
        return ContestDtos.SubmitVO.builder()
                .submitId(submission.getSubmitId())
                .result(submission.getResult())
                .build();
    }

    // ==================== 榜单 ====================

    public ContestDtos.LeaderboardVO leaderboard(Long contestId) {
        Contest contest = getOrThrow(contestId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = contest.getEndTime();
        LocalDateTime freezeCutoff = null;
        boolean frozen = false;
        int freezeMinutes = contest.getFreezeMinutes() == null ? 0 : contest.getFreezeMinutes();
        if (endTime != null && freezeMinutes > 0) {
            LocalDateTime freezeStart = endTime.minusMinutes(freezeMinutes);
            if (!now.isBefore(freezeStart) && now.isBefore(endTime)) {
                frozen = true;
                freezeCutoff = freezeStart;
            }
        }

        List<ContestRegistration> approved = registrationMapper.selectList(
                Wrappers.<ContestRegistration>lambdaQuery()
                        .eq(ContestRegistration::getContestId, contestId)
                        .eq(ContestRegistration::getStatus, Enums.ApplyStatus.APPROVED.name()));
        List<Long> registeredTeamIds = approved.stream()
                .map(ContestRegistration::getTeamId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ContestRows.TeamRow> teamMap = loadTeams(registeredTeamIds);
        Map<Long, ContestRows.TeamMemberRow> memberTeamMap = registeredTeamIds.isEmpty()
                ? Map.of()
                : queryMapper.selectTeamMembers(registeredTeamIds).stream()
                        .collect(Collectors.toMap(ContestRows.TeamMemberRow::getMemberId,
                                Function.identity(), (a, b) -> a));

        List<Long> memberIds = new ArrayList<>(approved.stream()
                .map(ContestRegistration::getMemberId).toList());
        memberIds.addAll(memberTeamMap.keySet());
        Map<Long, Member> memberMap = loadMembers(memberIds);

        Map<String, BoardGroup> groups = new LinkedHashMap<>();
        for (ContestRegistration registration : approved) {
            Member applicant = memberMap.get(registration.getMemberId());
            if (registration.getTeamId() != null) {
                ContestRows.TeamRow team = teamMap.get(registration.getTeamId());
                String key = "T" + registration.getTeamId();
                groups.computeIfAbsent(key, k -> BoardGroup.team(
                        registration.getTeamId(),
                        team == null ? "未知队伍" : team.getName(),
                        registration.getMemberId(),
                        applicant == null ? "未知会员" : applicant.getName()));
            } else {
                String key = "U" + registration.getMemberId();
                groups.computeIfAbsent(key, k -> BoardGroup.member(
                        registration.getMemberId(),
                        applicant == null ? "未知会员" : applicant.getName()));
            }
        }

        LambdaQueryWrapper<Submission> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Submission::getContestId, contestId)
                .orderByAsc(Submission::getSubmitTime)
                .orderByAsc(Submission::getSubmitId);
        List<Submission> submissions = submissionMapper.selectList(wrapper);
        if (freezeCutoff != null) {
            LocalDateTime cutoff = freezeCutoff;
            submissions = submissions.stream()
                    .filter(submission -> submission.getSubmitTime() != null
                            && !submission.getSubmitTime().isAfter(cutoff))
                    .toList();
        }

        LocalDateTime boardStart = contest.getStartTime();
        for (Submission submission : submissions) {
            String result = submission.getResult();
            boolean accepted = Enums.JudgeResult.AC.name().equals(result);
            boolean wrong = WRONG_RESULTS.contains(result);
            if (!accepted && !wrong) {
                // PENDING / JUDGING 不参与榜单计算。
                continue;
            }
            if (submission.getSubmitTime() == null) {
                continue;
            }

            Long memberId = submission.getSubmitterId();
            Member member = memberMap.get(memberId);
            ContestRows.TeamMemberRow team = memberTeamMap.get(memberId);
            String key = team != null && team.getTeamId() != null
                    ? "T" + team.getTeamId() : "U" + memberId;
            BoardGroup group = groups.get(key);
            if (group == null) {
                if (team != null) {
                    group = groups.computeIfAbsent(key, k -> BoardGroup.team(
                            team.getTeamId(), team.getTeamName(), memberId,
                            member == null ? "未知会员" : member.getName()));
                } else {
                    group = groups.computeIfAbsent(key, k -> BoardGroup.member(
                            memberId, member == null ? "未知会员" : member.getName()));
                }
            }

            ProblemState problemState = group.problems.computeIfAbsent(
                    submission.getProblemId(), k -> new ProblemState());
            if (problemState.solved) {
                continue;
            }
            if (accepted) {
                problemState.solved = true;
                LocalDateTime start = boardStart == null
                        ? submission.getSubmitTime() : boardStart;
                long minutes = Math.max(0,
                        Duration.between(start, submission.getSubmitTime()).toMinutes());
                group.penalty += minutes + 20L * problemState.wrong;
                group.solved++;
                group.lastAcceptedTime = submission.getSubmitTime();
            } else {
                problemState.wrong++;
            }
        }

        List<BoardGroup> sorted = groups.values().stream()
                .sorted(Comparator
                        .comparingInt((BoardGroup group) -> group.solved).reversed()
                        .thenComparingLong(group -> group.penalty)
                        .thenComparing(group -> group.lastAcceptedTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(group -> group.key))
                .toList();
        List<ContestDtos.LeaderboardEntry> entries = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            BoardGroup group = sorted.get(i);
            entries.add(ContestDtos.LeaderboardEntry.builder()
                    .rank(i + 1)
                    .userId(group.userId)
                    .userName(group.userName)
                    .teamId(group.teamId)
                    .teamName(group.teamName)
                    .solvedCount(group.solved)
                    .penalty((int) group.penalty)
                    .build());
        }
        return ContestDtos.LeaderboardVO.builder()
                .frozen(frozen)
                .entries(entries)
                .build();
    }

    // ==================== 成绩与证书 ====================

    @Transactional
    public void saveResults(Long contestId, ContestDtos.ResultRequest request) {
        getOrThrow(contestId);
        for (ContestDtos.ResultItem item : request.getResults()) {
            if (item == null || item.getRank() == null || item.getRank() < 1) {
                throw BusinessException.badRequest("成绩名次必须为正整数");
            }
            if (StringUtils.hasText(item.getAward()) && item.getAward().length() > 100) {
                throw BusinessException.badRequest("奖项名称长度不能超过 100");
            }
            if (item.getTeamId() != null && queryMapper.selectTeam(item.getTeamId()) == null) {
                throw BusinessException.notFound("队伍不存在：" + item.getTeamId());
            }

            LambdaQueryWrapper<ContestResult> wrapper = Wrappers.<ContestResult>lambdaQuery()
                    .eq(ContestResult::getContestId, contestId);
            if (item.getTeamId() != null) {
                wrapper.eq(ContestResult::getTeamId, item.getTeamId());
            } else {
                wrapper.isNull(ContestResult::getTeamId);
            }
            ContestResult result = resultMapper.selectOne(wrapper);
            if (result == null) {
                result = new ContestResult();
                result.setContestId(contestId);
                result.setTeamId(item.getTeamId());
            }
            result.setRankNo(item.getRank());
            result.setAward(item.getAward());
            result.setScore(item.getScore() == null ? BigDecimal.ZERO : item.getScore());
            if (result.getResultId() == null) {
                resultMapper.insert(result);
            } else {
                resultMapper.updateById(result);
            }
        }
        log.info("赛事 {} 登记成绩 {} 条", contestId, request.getResults().size());
    }

    public ContestDtos.CertificateVO certificates(Long contestId,
                                                  ContestDtos.CertificateRequest request) {
        getOrThrow(contestId);
        List<ContestResult> results = resultMapper.selectList(
                Wrappers.<ContestResult>lambdaQuery()
                        .eq(ContestResult::getContestId, contestId));

        Set<Long> matched = new HashSet<>();
        if (request.getAwardRules() != null) {
            for (ContestDtos.AwardRule rule : request.getAwardRules()) {
                int[] range = parseRankRange(rule.getRankRange());
                results.stream()
                        .filter(result -> result.getRankNo() != null
                                && result.getRankNo() >= range[0]
                                && result.getRankNo() <= range[1])
                        .forEach(result -> matched.add(result.getResultId()));
            }
        }

        // TODO 接入文件/模板模块后生成实际证书文件并返回下载地址。
        log.info("赛事 {} 证书统计：模板 {}，命中 {} 条",
                contestId, request.getTemplateId(), matched.size());
        return ContestDtos.CertificateVO.builder()
                .generatedCount(matched.size())
                .downloadUrl(null)
                .build();
    }

    // ==================== 内部工具 ====================

    private Contest getOrThrow(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw BusinessException.notFound("赛事不存在");
        }
        return contest;
    }

    private boolean isApprovedParticipant(Long contestId, Long memberId) {
        List<ContestRegistration> approved = registrationMapper.selectList(
                Wrappers.<ContestRegistration>lambdaQuery()
                        .eq(ContestRegistration::getContestId, contestId)
                        .eq(ContestRegistration::getStatus, Enums.ApplyStatus.APPROVED.name()));
        if (approved.stream().anyMatch(registration -> registration.getTeamId() == null
                && memberId.equals(registration.getMemberId()))) {
            return true;
        }
        List<Long> teamIds = approved.stream()
                .map(ContestRegistration::getTeamId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return !teamIds.isEmpty()
                && queryMapper.selectTeamMembers(teamIds).stream()
                        .anyMatch(row -> memberId.equals(row.getMemberId()));
    }

    private String effectiveStatus(Contest contest, LocalDateTime now) {
        if (contest.getEndTime() != null && !now.isBefore(contest.getEndTime())) {
            return Enums.ContestStatus.ENDED.name();
        }
        if (contest.getStartTime() != null && !now.isBefore(contest.getStartTime())) {
            return Enums.ContestStatus.ONGOING.name();
        }
        if (contest.getRegistrationDeadline() != null
                && !now.isAfter(contest.getRegistrationDeadline())) {
            return Enums.ContestStatus.REGISTERING.name();
        }
        return Enums.ContestStatus.ANNOUNCED.name();
    }

    private ContestDtos.ContestVO toVO(Contest contest, LocalDateTime now, Member creator,
                                       List<ContestRows.ContestProblemRow> problems) {
        ContestDtos.UserBrief creatorBrief = creator == null
                ? null
                : ContestDtos.UserBrief.builder()
                        .memberId(creator.getMemberId())
                        .studentId(creator.getStudentId())
                        .name(creator.getName())
                        .build();
        return ContestDtos.ContestVO.builder()
                .contestId(contest.getContestId())
                .name(contest.getName())
                .category(contest.getCategory())
                .startTime(contest.getStartTime())
                .endTime(contest.getEndTime())
                .registrationDeadline(contest.getRegistrationDeadline())
                .description(contest.getDescription())
                .requirements(contest.getRequirements())
                .maxTeamSize(contest.getMaxTeamSize())
                .freezeMinutes(contest.getFreezeMinutes())
                .status(effectiveStatus(contest, now))
                .problemIds(problems.stream()
                        .map(ContestRows.ContestProblemRow::getProblemId).toList())
                .creator(creatorBrief)
                .createTime(contest.getCreateTime())
                .updateTime(contest.getUpdateTime())
                .build();
    }

    private ContestDtos.RegistrationVO toRegistrationVO(
            ContestRegistration registration, Map<Long, Member> memberMap,
            Map<Long, ContestRows.TeamRow> teamMap) {
        Member applicant = memberMap.get(registration.getMemberId());
        Member auditor = registration.getAuditorId() == null
                ? null : memberMap.get(registration.getAuditorId());
        ContestRows.TeamRow team = registration.getTeamId() == null
                ? null : teamMap.get(registration.getTeamId());
        return ContestDtos.RegistrationVO.builder()
                .applyId(registration.getRegistrationId())
                .contestId(registration.getContestId())
                .teamId(registration.getTeamId())
                .teamName(team == null ? null : team.getName())
                .applicantId(registration.getMemberId())
                .applicantName(applicant == null ? null : applicant.getName())
                .studentId(applicant == null ? null : applicant.getStudentId())
                .applyData(registration.getApplyData())
                .status(registration.getStatus())
                .auditRemark(registration.getAuditRemark())
                .auditorId(registration.getAuditorId())
                .auditorName(auditor == null ? null : auditor.getName())
                .applyTime(registration.getApplyTime())
                .auditTime(registration.getAuditTime())
                .build();
    }

    private Map<Long, List<ContestRows.ContestProblemRow>> loadContestProblems(
            Collection<Long> contestIds) {
        if (contestIds.isEmpty()) {
            return Map.of();
        }
        return queryMapper.selectContestProblems(contestIds).stream()
                .collect(Collectors.groupingBy(ContestRows.ContestProblemRow::getContestId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, Member> loadMembers(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Member::getMemberId, Function.identity(),
                        (a, b) -> a));
    }

    private Map<Long, ContestRows.TeamRow> loadTeams(Collection<Long> teamIds) {
        List<Long> ids = teamIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return queryMapper.selectTeams(ids).stream()
                .collect(Collectors.toMap(ContestRows.TeamRow::getTeamId,
                        Function.identity(), (a, b) -> a));
    }

    private void saveContestProblems(Long contestId, List<Long> problemIds) {
        int sort = 0;
        for (Long problemId : problemIds) {
            ContestProblem link = new ContestProblem();
            link.setContestId(contestId);
            link.setProblemId(problemId);
            link.setSortOrder(sort++);
            contestProblemMapper.insert(link);
        }
    }

    private List<Long> distinct(List<Long> problemIds) {
        if (problemIds == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(problemIds));
    }

    private void validateProblems(List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return;
        }
        if (queryMapper.countExistingProblems(problemIds) != problemIds.size()) {
            throw BusinessException.badRequest("部分题目不存在或已被删除");
        }
    }

    private void validateContestTime(LocalDateTime startTime, LocalDateTime endTime,
                                     LocalDateTime registrationDeadline) {
        if (!startTime.isBefore(endTime)) {
            throw BusinessException.badRequest("开始时间必须早于结束时间");
        }
        if (registrationDeadline.isAfter(endTime)) {
            throw BusinessException.badRequest("报名截止时间不能晚于赛事结束时间");
        }
    }

    private int[] parseRankRange(String rankRange) {
        String text = rankRange == null ? "" : rankRange.trim();
        try {
            if (text.contains("-")) {
                String[] parts = text.split("-");
                if (parts.length != 2) {
                    throw new NumberFormatException(text);
                }
                int low = Integer.parseInt(parts[0].trim());
                int high = Integer.parseInt(parts[1].trim());
                if (low < 1 || high < low) {
                    throw new NumberFormatException(text);
                }
                return new int[]{low, high};
            }
            int rank = Integer.parseInt(text);
            if (rank < 1) {
                throw new NumberFormatException(text);
            }
            return new int[]{rank, rank};
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest("获奖名次区间格式不合法：" + rankRange);
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw BusinessException.badRequest(field + " 取值不合法：" + value);
        }
    }

    private static final class ProblemState {
        private boolean solved;
        private int wrong;
    }

    private static final class BoardGroup {
        private String key;
        private Long userId;
        private String userName;
        private Long teamId;
        private String teamName;
        private int solved;
        private long penalty;
        private LocalDateTime lastAcceptedTime;
        private final Map<Long, ProblemState> problems = new LinkedHashMap<>();

        private static BoardGroup team(Long teamId, String teamName,
                                       Long leaderId, String leaderName) {
            BoardGroup group = new BoardGroup();
            group.key = "T" + teamId;
            group.teamId = teamId;
            group.teamName = teamName;
            group.userId = leaderId;
            group.userName = leaderName;
            return group;
        }

        private static BoardGroup member(Long memberId, String memberName) {
            BoardGroup group = new BoardGroup();
            group.key = "U" + memberId;
            group.userId = memberId;
            group.userName = memberName;
            return group;
        }
    }
}
