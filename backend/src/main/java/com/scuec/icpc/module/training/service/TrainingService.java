package com.scuec.icpc.module.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.enums.Enums;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.training.dto.TrainingDtos;
import com.scuec.icpc.module.training.dto.TrainingRows;
import com.scuec.icpc.module.problem.entity.ProblemLink;
import com.scuec.icpc.module.problem.mapper.ProblemLinkMapper;
import com.scuec.icpc.module.training.entity.Submission;
import com.scuec.icpc.module.training.entity.Training;
import com.scuec.icpc.module.training.mapper.SubmissionMapper;
import com.scuec.icpc.module.training.mapper.TrainingMapper;
import com.scuec.icpc.module.training.mapper.TrainingQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 训练管理业务：训练 CRUD、发布、代码提交、ICPC 榜单与个人能力评估。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {

    private static final List<String> WRONG_RESULTS =
            List.of("WA", "TLE", "MLE", "RE", "CE");

    private final TrainingMapper trainingMapper;
    private final ProblemLinkMapper problemLinkMapper;
    private final SubmissionMapper submissionMapper;
    private final TrainingQueryMapper queryMapper;
    private final MemberMapper memberMapper;

    // ==================== 训练管理 ====================

    public PageResult<TrainingDtos.TrainingVO> page(TrainingDtos.Query query) {
        LocalDateTime now = LocalDateTime.now();
        boolean manager = SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH);

        LambdaQueryWrapper<Training> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Training::getType, parseEnum(Enums.TrainingType.class,
                    query.getType(), "type").name());
        }
        if (StringUtils.hasText(query.getStatus())) {
            applyStatusFilter(wrapper, parseEnum(Enums.TrainingStatus.class,
                    query.getStatus(), "status"), now);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Training::getName, query.getKeyword().trim());
        }
        // 普通会员不可见草稿训练。
        if (!manager) {
            wrapper.ne(Training::getStatus, Enums.TrainingStatus.DRAFT.name());
        }
        wrapper.orderByDesc(Training::getCreateTime).orderByDesc(Training::getTrainingId);

        Page<Training> page = trainingMapper.selectPage(query.toPage(), wrapper);
        List<Long> trainingIds = page.getRecords().stream()
                .map(Training::getTrainingId).toList();
        Map<Long, List<Long>> problemIdsMap = loadProblemIds(trainingIds);
        Map<Long, Member> creatorMap = loadCreators(page.getRecords().stream()
                .map(Training::getCreatorId).toList());

        return PageResult.of(page, training -> toVO(training, now,
                problemIdsMap.getOrDefault(training.getTrainingId(), List.of()),
                creatorMap.get(training.getCreatorId())));
    }

    public TrainingDtos.TrainingDetailVO detail(Long trainingId) {
        Training training = getOrThrow(trainingId);
        LocalDateTime now = LocalDateTime.now();
        if (Enums.TrainingStatus.DRAFT.name().equals(training.getStatus())
                && !SecurityUtils.isSelf(training.getCreatorId())
                && !SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH)) {
            throw BusinessException.forbidden("草稿训练仅创建者与教练组可见");
        }

        List<TrainingDtos.ProblemItem> problems = queryMapper.selectTrainingProblems(trainingId);
        attachTags(problems, queryMapper.selectTrainingProblemTags(trainingId));
        Member creator = training.getCreatorId() == null
                ? null : memberMapper.selectById(training.getCreatorId());
        return TrainingDtos.TrainingDetailVO.builder()
                .basicInfo(toVO(training, now,
                        problems.stream().map(TrainingDtos.ProblemItem::getProblemId).toList(),
                        creator))
                .problems(problems)
                .build();
    }

    @Transactional
    public TrainingDtos.TrainingVO create(TrainingDtos.CreateRequest request) {
        String type = parseEnum(Enums.TrainingType.class, request.getType(), "type").name();
        validateTime(request.getStartTime(), request.getEndTime());
        List<Long> problemIds = distinctProblems(request.getProblemIds());
        validateProblems(problemIds);

        Training training = new Training();
        training.setName(request.getName().trim());
        training.setType(type);
        training.setDescription(request.getDescription());
        training.setStartTime(request.getStartTime());
        training.setEndTime(request.getEndTime());
        training.setLeaderboardVisible(request.getLeaderboardVisible() == null
                || request.getLeaderboardVisible());
        training.setFreezeTime(nvl(request.getFreezeTime()));
        training.setStatus(Enums.TrainingStatus.DRAFT.name());
        training.setCreatorId(SecurityUtils.currentMemberId());
        trainingMapper.insert(training);
        saveProblems(training.getTrainingId(), problemIds);

        log.info("创建训练 {}（{}），共 {} 道题目",
                training.getTrainingId(), training.getName(), problemIds.size());
        return toVO(training, LocalDateTime.now(), problemIds,
                memberMapper.selectById(training.getCreatorId()));
    }

    @Transactional
    public TrainingDtos.TrainingVO update(Long trainingId, TrainingDtos.UpdateRequest request) {
        Training training = getOrThrow(trainingId);
        requireOwnerOrMinister(training, "修改训练");
        if (!Enums.TrainingStatus.DRAFT.name().equals(training.getStatus())) {
            throw BusinessException.conflict("仅草稿状态的训练可以修改");
        }

        LocalDateTime startTime = request.getStartTime() == null
                ? training.getStartTime() : request.getStartTime();
        LocalDateTime endTime = request.getEndTime() == null
                ? training.getEndTime() : request.getEndTime();
        validateTime(startTime, endTime);

        if (StringUtils.hasText(request.getName())) {
            training.setName(request.getName().trim());
        }
        training.setStartTime(startTime);
        training.setEndTime(endTime);
        if (request.getDescription() != null) {
            training.setDescription(request.getDescription());
        }
        if (request.getLeaderboardVisible() != null) {
            training.setLeaderboardVisible(request.getLeaderboardVisible());
        }
        if (request.getFreezeTime() != null) {
            training.setFreezeTime(request.getFreezeTime());
        }

        List<Long> problemIds = null;
        if (request.getProblemIds() != null) {
            problemIds = distinctProblems(request.getProblemIds());
            if (problemIds.isEmpty()) {
                throw BusinessException.badRequest("训练题目不能为空");
            }
            validateProblems(problemIds);
            removeProblemLinks(trainingId);
            saveProblems(trainingId, problemIds);
        }

        trainingMapper.updateById(training);
        List<Long> effectiveProblemIds = problemIds != null
                ? problemIds
                : loadProblemIds(List.of(trainingId)).getOrDefault(trainingId, List.of());
        return toVO(training, LocalDateTime.now(), effectiveProblemIds,
                memberMapper.selectById(training.getCreatorId()));
    }

    @Transactional
    public void delete(Long trainingId) {
        Training training = getOrThrow(trainingId);
        requireOwnerOrMinister(training, "删除训练");
        if (!Enums.TrainingStatus.DRAFT.name().equals(training.getStatus())) {
            throw BusinessException.conflict("仅草稿状态的训练可以删除");
        }
        trainingMapper.deleteById(trainingId);
        removeProblemLinks(trainingId);
    }

    @Transactional
    public TrainingDtos.TrainingVO publish(Long trainingId) {
        Training training = getOrThrow(trainingId);
        requireOwnerOrMinister(training, "发布训练");
        if (!Enums.TrainingStatus.DRAFT.name().equals(training.getStatus())) {
            throw BusinessException.conflict("仅草稿状态的训练可以发布");
        }
        validateTime(training.getStartTime(), training.getEndTime());
        List<Long> problemIds = loadProblemIds(List.of(trainingId))
                .getOrDefault(trainingId, List.of());
        if (problemIds.isEmpty()) {
            throw BusinessException.badRequest("训练至少需要包含一道题目");
        }

        training.setStatus(Enums.TrainingStatus.PUBLISHED.name());
        trainingMapper.updateById(training);
        log.info("发布训练 {}（{}）", trainingId, training.getName());
        return toVO(training, LocalDateTime.now(), problemIds,
                memberMapper.selectById(training.getCreatorId()));
    }

    // ==================== 提交记录 ====================

    @Transactional
    public TrainingDtos.SubmissionVO submit(Long trainingId, TrainingDtos.SubmitRequest request) {
        Training training = getOrThrow(trainingId);
        LocalDateTime now = LocalDateTime.now();
        if (!Enums.TrainingStatus.ONGOING.name()
                .equals(effectiveStatus(training, now))) {
            throw BusinessException.badRequest("训练不在进行中，暂不能提交代码");
        }

        List<TrainingDtos.ProblemItem> problems = queryMapper.selectTrainingProblems(trainingId);
        TrainingDtos.ProblemItem problem = problems.stream()
                .filter(item -> item.getProblemId().equals(request.getProblemId()))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("该题目不属于本次训练"));

        Member submitter = memberMapper.selectById(SecurityUtils.currentMemberId());
        if (submitter == null || !Enums.MemberStatus.ACTIVE.name().equals(submitter.getStatus())) {
            throw BusinessException.forbidden("当前会员状态不允许提交代码");
        }

        Submission submission = new Submission();
        submission.setTrainingId(trainingId);
        submission.setProblemId(request.getProblemId());
        submission.setSubmitterId(submitter.getMemberId());
        submission.setCode(request.getCode());
        submission.setLanguage(request.getLanguage().trim());
        submission.setResult(Enums.JudgeResult.PENDING.name());
        submission.setSubmitTime(now);
        submissionMapper.insert(submission);

        // TODO 接入 OJ 判题：将 submission 投递到判题队列，异步回写 result/timeUsed/memoryUsed。
        log.info("会员 {} 提交训练 {} 题目 {}，提交记录 {}",
                submitter.getMemberId(), trainingId, request.getProblemId(),
                submission.getSubmitId());
        return toSubmissionVO(submission, submitter, problem.getTitle());
    }

    public List<TrainingDtos.SubmissionVO> submissions(Long trainingId,
                                                       TrainingDtos.SubmissionQuery query) {
        getOrThrow(trainingId);
        Long callerId = SecurityUtils.currentMemberId();
        boolean manager = SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH);

        Long targetUserId = query.getUserId();
        if (targetUserId != null) {
            if (!targetUserId.equals(callerId) && !manager) {
                throw BusinessException.forbidden("只能查看自己的提交记录");
            }
        } else if (!manager) {
            targetUserId = callerId;
        }

        LambdaQueryWrapper<Submission> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Submission::getTrainingId, trainingId);
        if (targetUserId != null) {
            wrapper.eq(Submission::getSubmitterId, targetUserId);
        }
        if (query.getProblemId() != null) {
            wrapper.eq(Submission::getProblemId, query.getProblemId());
        }
        if (StringUtils.hasText(query.getResult())) {
            wrapper.eq(Submission::getResult, parseEnum(Enums.JudgeResult.class,
                    query.getResult(), "result").name());
        }
        wrapper.orderByDesc(Submission::getSubmitTime)
                .orderByDesc(Submission::getSubmitId)
                .last("LIMIT 500");
        List<Submission> submissions = submissionMapper.selectList(wrapper);
        if (submissions.isEmpty()) {
            return List.of();
        }

        Map<Long, Member> memberMap = loadMembers(submissions.stream()
                .map(Submission::getSubmitterId).toList());
        Map<Long, String> problemTitleMap = queryMapper.selectTrainingProblems(trainingId)
                .stream()
                .collect(Collectors.toMap(TrainingDtos.ProblemItem::getProblemId,
                        TrainingDtos.ProblemItem::getTitle, (a, b) -> a));
        return submissions.stream()
                .map(submission -> toSubmissionVO(submission,
                        memberMap.get(submission.getSubmitterId()),
                        problemTitleMap.get(submission.getProblemId()),
                        false))
                .toList();
    }

    // ==================== 榜单与能力评估 ====================

    public TrainingDtos.LeaderboardVO leaderboard(Long trainingId) {
        Training training = getOrThrow(trainingId);
        boolean manager = SecurityUtils.isAtLeast(Roles.OFFICER);
        if (Boolean.FALSE.equals(training.getLeaderboardVisible()) && !manager) {
            throw BusinessException.forbidden("本次训练榜单暂未公开");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = training.getEndTime();
        LocalDateTime freezeCutoff = null;
        boolean frozen = false;
        if (endTime != null && nvl(training.getFreezeTime()) > 0) {
            LocalDateTime freezeStart = endTime.minusMinutes(training.getFreezeTime());
            if (!now.isBefore(freezeStart) && now.isBefore(endTime)) {
                frozen = true;
                freezeCutoff = freezeStart;
            }
        }

        LambdaQueryWrapper<Submission> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Submission::getTrainingId, trainingId)
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

        List<Long> submitterIds = submissions.stream()
                .map(Submission::getSubmitterId).distinct().toList();
        Map<Long, Member> memberMap = loadMembers(submitterIds);
        Map<Long, TrainingRows.TeamMemberRow> teamMap =
                Enums.TrainingType.TEAM.name().equals(training.getType())
                        ? loadTeamMap(submitterIds) : Map.of();

        LocalDateTime boardStart = training.getStartTime();
        Map<String, BoardGroup> groups = new LinkedHashMap<>();
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
            TrainingRows.TeamMemberRow team = teamMap.get(memberId);
            String key;
            BoardGroup group;
            if (team != null && team.getTeamId() != null) {
                key = "T" + team.getTeamId();
                group = groups.computeIfAbsent(key, k -> BoardGroup.team(
                        team.getTeamId(), team.getTeamName()));
            } else {
                key = "U" + memberId;
                Member member = memberMap.get(memberId);
                group = groups.computeIfAbsent(key, k -> BoardGroup.member(
                        memberId, member == null ? "未知会员" : member.getName()));
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

        List<BoardGroup> sortedGroups = groups.values().stream()
                .sorted(Comparator
                        .comparingInt((BoardGroup group) -> group.solved).reversed()
                        .thenComparingLong(group -> group.penalty)
                        .thenComparing(group -> group.lastAcceptedTime,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<TrainingDtos.LeaderboardEntry> entries = new ArrayList<>(sortedGroups.size());
        for (int i = 0; i < sortedGroups.size(); i++) {
            BoardGroup group = sortedGroups.get(i);
            entries.add(TrainingDtos.LeaderboardEntry.builder()
                    .rank(i + 1)
                    .userId(group.userId)
                    .userName(group.userName)
                    .teamId(group.teamId)
                    .teamName(group.teamName)
                    .solvedCount(group.solved)
                    .penalty(group.penalty)
                    .build());
        }

        return TrainingDtos.LeaderboardVO.builder()
                .frozen(frozen)
                .entries(entries)
                .build();
    }

    public TrainingDtos.AbilityVO ability(Long memberId) {
        SecurityUtils.requireSelfOrAtLeast(memberId, Roles.ASSISTANT_COACH);
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.notFound("会员不存在");
        }

        List<TrainingDtos.AbilityDimension> dimensions =
                queryMapper.selectAbilityByMember(memberId).stream()
                        .map(row -> {
                            int solved = nvl(row.getSolvedCount());
                            int attempt = nvl(row.getAttemptCount());
                            double accuracy = attempt == 0 ? 0 : (double) solved / attempt;
                            double volume = Math.min(1.0, solved / 10.0);
                            double score = Math.round(
                                    (70 * accuracy + 30 * volume) * 10.0) / 10.0;
                            return TrainingDtos.AbilityDimension.builder()
                                    .tagName(row.getTagName())
                                    .score(score)
                                    .solvedCount(solved)
                                    .attemptCount(attempt)
                                    .build();
                        })
                        .sorted(Comparator.comparingDouble(
                                TrainingDtos.AbilityDimension::getScore).reversed())
                        .toList();

        List<TrainingDtos.TrendPoint> trend = queryMapper.selectRecentTrend(memberId).stream()
                .map(row -> TrainingDtos.TrendPoint.builder()
                        .day(row.getDay())
                        .acCount(nvl(row.getAcCount()))
                        .submitCount(nvl(row.getSubmitCount()))
                        .build())
                .toList();
        int totalSolved = queryMapper.countSolvedProblems(memberId);

        return TrainingDtos.AbilityVO.builder()
                .tagScores(dimensions)
                .recentTrend(trend)
                .totalSolved(totalSolved)
                .report(buildAbilityReport(member, dimensions, totalSolved))
                .build();
    }

    // ==================== 内部工具 ====================

    private void applyStatusFilter(LambdaQueryWrapper<Training> wrapper,
                                   Enums.TrainingStatus status, LocalDateTime now) {
        String draft = Enums.TrainingStatus.DRAFT.name();
        switch (status) {
            case DRAFT -> wrapper.eq(Training::getStatus, draft);
            case PUBLISHED -> wrapper.ne(Training::getStatus, draft)
                    .gt(Training::getStartTime, now);
            case ONGOING -> wrapper.ne(Training::getStatus, draft)
                    .le(Training::getStartTime, now)
                    .gt(Training::getEndTime, now);
            case ENDED -> wrapper.ne(Training::getStatus, draft)
                    .le(Training::getEndTime, now);
        }
    }

    private Training getOrThrow(Long trainingId) {
        Training training = trainingMapper.selectById(trainingId);
        if (training == null) {
            throw BusinessException.notFound("训练不存在");
        }
        return training;
    }

    /**
     * 训练修改、发布、删除仅创建者或部长及以上角色可操作。
     */
    private void requireOwnerOrMinister(Training training, String action) {
        if (SecurityUtils.isSelf(training.getCreatorId())
                || SecurityUtils.isAtLeast(Roles.MINISTER)) {
            return;
        }
        throw BusinessException.forbidden("仅训练创建者或部长及以上角色可" + action);
    }

    private String effectiveStatus(Training training, LocalDateTime now) {
        if (Enums.TrainingStatus.DRAFT.name().equals(training.getStatus())) {
            return Enums.TrainingStatus.DRAFT.name();
        }
        if (training.getEndTime() != null && !now.isBefore(training.getEndTime())) {
            return Enums.TrainingStatus.ENDED.name();
        }
        if (training.getStartTime() != null && !now.isBefore(training.getStartTime())) {
            return Enums.TrainingStatus.ONGOING.name();
        }
        return Enums.TrainingStatus.PUBLISHED.name();
    }

    private TrainingDtos.TrainingVO toVO(Training training, LocalDateTime now,
                                         List<Long> problemIds, Member creator) {
        TrainingDtos.UserBrief creatorBrief = creator == null
                ? null
                : TrainingDtos.UserBrief.builder()
                        .memberId(creator.getMemberId())
                        .studentId(creator.getStudentId())
                        .name(creator.getName())
                        .build();
        return TrainingDtos.TrainingVO.builder()
                .trainingId(training.getTrainingId())
                .name(training.getName())
                .type(training.getType())
                .description(training.getDescription())
                .startTime(training.getStartTime())
                .endTime(training.getEndTime())
                .leaderboardVisible(training.getLeaderboardVisible())
                .freezeTime(training.getFreezeTime())
                .problemIds(problemIds)
                .status(effectiveStatus(training, now))
                .creator(creatorBrief)
                .createTime(training.getCreateTime())
                .updateTime(training.getUpdateTime())
                .build();
    }

    private TrainingDtos.SubmissionVO toSubmissionVO(Submission submission, Member submitter,
                                                     String problemTitle) {
        return toSubmissionVO(submission, submitter, problemTitle, true);
    }

    private TrainingDtos.SubmissionVO toSubmissionVO(Submission submission, Member submitter,
                                                     String problemTitle, boolean includeCode) {
        return TrainingDtos.SubmissionVO.builder()
                .submitId(submission.getSubmitId())
                .trainingId(submission.getTrainingId())
                .problemId(submission.getProblemId())
                .problemTitle(problemTitle)
                .submitterId(submission.getSubmitterId())
                .submitterName(submitter == null ? null : submitter.getName())
                .code(includeCode ? submission.getCode() : null)
                .language(submission.getLanguage())
                .result(submission.getResult())
                .timeUsed(submission.getTimeUsed())
                .memoryUsed(submission.getMemoryUsed())
                .submitTime(submission.getSubmitTime())
                .build();
    }

    private void attachTags(List<TrainingDtos.ProblemItem> problems,
                            List<TrainingRows.ProblemTagRow> tags) {
        Map<Long, List<TrainingDtos.TagItem>> tagMap = tags.stream()
                .collect(Collectors.groupingBy(TrainingRows.ProblemTagRow::getProblemId,
                        Collectors.mapping(row -> TrainingDtos.TagItem.builder()
                                        .tagId(row.getTagId())
                                        .name(row.getName())
                                        .category(row.getCategory())
                                        .build(),
                                Collectors.toList())));
        problems.forEach(problem -> problem.setTags(
                tagMap.getOrDefault(problem.getProblemId(), List.of())));
    }

    private Map<Long, List<Long>> loadProblemIds(Collection<Long> trainingIds) {
        if (trainingIds.isEmpty()) {
            return Map.of();
        }
        List<ProblemLink> links = problemLinkMapper.selectList(
                Wrappers.<ProblemLink>lambdaQuery()
                        .eq(ProblemLink::getTargetType, Enums.ProblemTargetType.TRAINING.name())
                        .in(ProblemLink::getTargetId, trainingIds)
                        .orderByAsc(ProblemLink::getSortOrder)
                        .orderByAsc(ProblemLink::getId));
        Map<Long, List<Long>> result = new HashMap<>();
        for (ProblemLink link : links) {
            result.computeIfAbsent(link.getTargetId(), k -> new ArrayList<>())
                    .add(link.getProblemId());
        }
        return result;
    }

    private Map<Long, Member> loadCreators(Collection<Long> creatorIds) {
        return loadMembers(creatorIds);
    }

    private Map<Long, Member> loadMembers(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Member::getMemberId, Function.identity(),
                        (a, b) -> a));
    }

    private Map<Long, TrainingRows.TeamMemberRow> loadTeamMap(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return queryMapper.selectTeamsByMemberIds(ids).stream()
                .collect(Collectors.toMap(TrainingRows.TeamMemberRow::getMemberId,
                        Function.identity(), (a, b) -> a));
    }

    private void removeProblemLinks(Long trainingId) {
        problemLinkMapper.delete(Wrappers.<ProblemLink>lambdaQuery()
                .eq(ProblemLink::getTargetType, Enums.ProblemTargetType.TRAINING.name())
                .eq(ProblemLink::getTargetId, trainingId));
    }

    private void saveProblems(Long trainingId, List<Long> problemIds) {
        int sort = 0;
        for (Long problemId : problemIds) {
            ProblemLink link = new ProblemLink();
            link.setTargetType(Enums.ProblemTargetType.TRAINING.name());
            link.setTargetId(trainingId);
            link.setProblemId(problemId);
            link.setSortOrder(sort++);
            problemLinkMapper.insert(link);
        }
    }

    private List<Long> distinctProblems(List<Long> problemIds) {
        if (problemIds == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(problemIds));
    }

    private void validateProblems(List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            throw BusinessException.badRequest("训练题目不能为空");
        }
        long existing = queryMapper.countExistingProblems(problemIds);
        if (existing != problemIds.size()) {
            throw BusinessException.badRequest("部分题目不存在或已被删除");
        }
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw BusinessException.badRequest("训练开始时间与结束时间不能为空");
        }
        if (!startTime.isBefore(endTime)) {
            throw BusinessException.badRequest("训练开始时间必须早于结束时间");
        }
    }

    private String buildAbilityReport(Member member,
                                      List<TrainingDtos.AbilityDimension> dimensions,
                                      int totalSolved) {
        if (dimensions.isEmpty()) {
            return member.getName() + " 暂无训练提交记录，完成训练后即可生成能力画像。";
        }
        String strengths = dimensions.stream().limit(3)
                .map(item -> item.getTagName() + "（"
                        + String.format(Locale.ROOT, "%.1f", item.getScore()) + "分）")
                .collect(Collectors.joining("、"));
        List<TrainingDtos.AbilityDimension> weaknesses = dimensions.stream()
                .filter(item -> item.getScore() < 60)
                .sorted(Comparator.comparingDouble(TrainingDtos.AbilityDimension::getScore))
                .limit(3)
                .toList();
        String weakText = weaknesses.isEmpty()
                ? "暂无明显薄弱知识点"
                : weaknesses.stream()
                        .map(item -> item.getTagName() + "（"
                                + String.format(Locale.ROOT, "%.1f", item.getScore())
                                + "分）")
                        .collect(Collectors.joining("、"));
        return String.format(Locale.ROOT,
                "%s 累计通过 %d 道题目。优势知识点：%s；建议加强：%s。"
                        + "建议结合训练榜单复盘提交记录，针对薄弱知识点安排专项训练。",
                member.getName(), totalSolved, strengths, weakText);
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw BusinessException.badRequest(field + " 取值不合法：" + value);
        }
    }

    /**
     * 榜单聚合分组：SOLO 按会员聚合，TEAM 按队伍聚合。
     */
    private static final class BoardGroup {
        private Long userId;
        private String userName;
        private Long teamId;
        private String teamName;
        private int solved;
        private long penalty;
        private LocalDateTime lastAcceptedTime;
        private final Map<Long, ProblemState> problems = new HashMap<>();

        static BoardGroup member(Long userId, String userName) {
            BoardGroup group = new BoardGroup();
            group.userId = userId;
            group.userName = userName;
            return group;
        }

        static BoardGroup team(Long teamId, String teamName) {
            BoardGroup group = new BoardGroup();
            group.teamId = teamId;
            group.teamName = teamName;
            return group;
        }
    }

    private static final class ProblemState {
        private boolean solved;
        private int wrong;
    }
}
