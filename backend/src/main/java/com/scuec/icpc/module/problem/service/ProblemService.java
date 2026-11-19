package com.scuec.icpc.module.problem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.enums.Enums;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.problem.dto.ProblemDtos;
import com.scuec.icpc.module.problem.dto.ProblemRows;
import com.scuec.icpc.module.problem.entity.Problem;
import com.scuec.icpc.module.problem.entity.ProblemLink;
import com.scuec.icpc.module.problem.entity.ProblemTag;
import com.scuec.icpc.module.problem.entity.Solution;
import com.scuec.icpc.module.problem.mapper.ProblemLinkMapper;
import com.scuec.icpc.module.problem.mapper.ProblemMapper;
import com.scuec.icpc.module.problem.mapper.ProblemQueryMapper;
import com.scuec.icpc.module.problem.mapper.ProblemTagMapper;
import com.scuec.icpc.module.problem.mapper.SolutionMapper;
import com.scuec.icpc.module.tag.entity.Tag;
import com.scuec.icpc.module.tag.mapper.TagMapper;
import com.scuec.icpc.module.training.entity.Submission;
import com.scuec.icpc.module.training.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库业务：题目增删改查、题解维护以及题库内提交记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemMapper problemMapper;
    private final ProblemTagMapper problemTagMapper;
    private final SolutionMapper solutionMapper;
    private final ProblemLinkMapper problemLinkMapper;
    private final ProblemQueryMapper queryMapper;
    private final TagMapper tagMapper;
    private final SubmissionMapper submissionMapper;
    private final MemberMapper memberMapper;

    // ==================== 查询 ====================

    public PageResult<ProblemDtos.ProblemVO> page(ProblemDtos.Query query) {
        LambdaQueryWrapper<Problem> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Problem::getTitle, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getDifficulty())) {
            wrapper.eq(Problem::getDifficulty,
                    parseEnum(Enums.Difficulty.class, query.getDifficulty(), "difficulty").name());
        }
        if (StringUtils.hasText(query.getVisibility())) {
            wrapper.eq(Problem::getVisibility,
                    parseEnum(Enums.Visibility.class, query.getVisibility(), "visibility").name());
        }
        if (query.getTagId() != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM problem_tag pt "
                    + "WHERE pt.problem_id = problem.problem_id AND pt.tag_id = {0})", query.getTagId());
        }
        wrapper.orderByDesc(Problem::getCreateTime).orderByDesc(Problem::getProblemId);

        Page<Problem> page = problemMapper.selectPage(query.toPage(), wrapper);
        Map<Long, List<ProblemDtos.TagItem>> tagMap = loadTags(page.getRecords().stream()
                .map(Problem::getProblemId).toList());
        return PageResult.of(page, problem -> toVO(problem,
                tagMap.getOrDefault(problem.getProblemId(), List.of()), null));
    }

    public ProblemDtos.ProblemVO detail(Long problemId) {
        Problem problem = getOrThrow(problemId);
        List<ProblemDtos.TagItem> tags = loadTags(List.of(problemId))
                .getOrDefault(problemId, List.of());
        Solution solution = findSolution(problemId);
        return toVO(problem, tags, solution == null ? null : toSolutionVO(solution));
    }

    // ==================== 写操作 ====================

    @Transactional
    public ProblemDtos.ProblemVO create(ProblemDtos.CreateRequest request) {
        String difficulty = parseEnum(Enums.Difficulty.class,
                request.getDifficulty(), "difficulty").name();
        String visibility = StringUtils.hasText(request.getVisibility())
                ? parseEnum(Enums.Visibility.class, request.getVisibility(), "visibility").name()
                : Enums.Visibility.PUBLIC.name();
        List<Long> tagIds = distinctTagIds(request.getTagIds());

        Problem problem = new Problem();
        problem.setTitle(request.getTitle().trim());
        problem.setOjProblemId(trimToNull(request.getOjProblemId()));
        problem.setDifficulty(difficulty);
        problem.setTimeLimit(request.getTimeLimit());
        problem.setMemoryLimit(request.getMemoryLimit());
        problem.setDescription(request.getDescription());
        problem.setInputFormat(request.getInputFormat());
        problem.setOutputFormat(request.getOutputFormat());
        problem.setSampleInput(request.getSampleInput());
        problem.setSampleOutput(request.getSampleOutput());
        problem.setHint(trimToNull(request.getHint()));
        problem.setVisibility(visibility);
        problem.setCreatorId(SecurityUtils.currentMemberId());
        problemMapper.insert(problem);
        saveTags(problem.getProblemId(), tagIds);

        log.info("新增题目 {}（{}），知识点 {} 个",
                problem.getProblemId(), problem.getTitle(), tagIds.size());
        return toVO(problem, loadTags(List.of(problem.getProblemId()))
                .getOrDefault(problem.getProblemId(), List.of()), null);
    }

    @Transactional
    public void update(Long problemId, ProblemDtos.UpdateRequest request) {
        Problem problem = getOrThrow(problemId);

        if (StringUtils.hasText(request.getTitle())) {
            problem.setTitle(request.getTitle().trim());
        }
        if (request.getOjProblemId() != null) {
            problem.setOjProblemId(trimToNull(request.getOjProblemId()));
        }
        if (StringUtils.hasText(request.getDifficulty())) {
            problem.setDifficulty(parseEnum(Enums.Difficulty.class,
                    request.getDifficulty(), "difficulty").name());
        }
        if (StringUtils.hasText(request.getVisibility())) {
            problem.setVisibility(parseEnum(Enums.Visibility.class,
                    request.getVisibility(), "visibility").name());
        }
        if (request.getTimeLimit() != null) {
            problem.setTimeLimit(request.getTimeLimit());
        }
        if (request.getMemoryLimit() != null) {
            problem.setMemoryLimit(request.getMemoryLimit());
        }
        if (request.getDescription() != null) {
            problem.setDescription(request.getDescription());
        }
        if (request.getInputFormat() != null) {
            problem.setInputFormat(request.getInputFormat());
        }
        if (request.getOutputFormat() != null) {
            problem.setOutputFormat(request.getOutputFormat());
        }
        if (request.getSampleInput() != null) {
            problem.setSampleInput(request.getSampleInput());
        }
        if (request.getSampleOutput() != null) {
            problem.setSampleOutput(request.getSampleOutput());
        }
        problemMapper.updateById(problem);

        if (request.getTagIds() != null) {
            saveTags(problemId, distinctTagIds(request.getTagIds()));
        }
        log.info("修改题目 {}", problemId);
    }

    @Transactional
    public void delete(Long problemId) {
        getOrThrow(problemId);
        problemMapper.deleteById(problemId);
        // 关联表没有逻辑删除字段，直接物理清理，避免残留脏数据
        problemTagMapper.delete(Wrappers.<ProblemTag>lambdaQuery()
                .eq(ProblemTag::getProblemId, problemId));
        problemLinkMapper.delete(Wrappers.<ProblemLink>lambdaQuery()
                .eq(ProblemLink::getProblemId, problemId));
        Solution solution = findSolution(problemId);
        if (solution != null) {
            solutionMapper.deleteById(solution.getSolutionId());
        }
        log.info("删除题目 {}", problemId);
    }

    @Transactional
    public void updateSolution(Long problemId, ProblemDtos.SolutionRequest request) {
        getOrThrow(problemId);
        Solution solution = findSolution(problemId);
        if (solution == null) {
            Solution created = new Solution();
            created.setProblemId(problemId);
            created.setContent(request.getContent());
            created.setReferenceCode(trimToNull(request.getReferenceCode()));
            solutionMapper.insert(created);
            log.info("新增题目 {} 的题解 {}", problemId, created.getSolutionId());
            return;
        }
        // 显式 set，允许把参考代码清空为 null
        solutionMapper.update(null, Wrappers.<Solution>lambdaUpdate()
                .set(Solution::getContent, request.getContent())
                .set(Solution::getReferenceCode, trimToNull(request.getReferenceCode()))
                .set(Solution::getUpdateTime, LocalDateTime.now())
                .eq(Solution::getSolutionId, solution.getSolutionId()));
        log.info("更新题目 {} 的题解", problemId);
    }

    /**
     * 从 OJ 批量导入。项目尚未配置自建 OJ 的题面拉取接口，无法生成真实题目数据，
     * 因此这里明确拒绝，避免把占位数据写入题库。
     */
    public ProblemDtos.ImportResult importFromOj(ProblemDtos.ImportRequest request) {
        throw BusinessException.badRequest("OJ 导入服务尚未接入，请先在 OJ 侧提供开放接口或手动录入题目");
    }

    // ==================== 题库内提交 ====================

    @Transactional
    public ProblemDtos.SubmitVO submit(Long problemId, ProblemDtos.SubmitRequest request) {
        getOrThrow(problemId);
        Long memberId = SecurityUtils.currentMemberId();
        Member member = memberMapper.selectById(memberId);
        if (member == null || !Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.forbidden("当前会员状态不允许提交代码");
        }

        Submission submission = new Submission();
        submission.setProblemId(problemId);
        submission.setSubmitterId(memberId);
        submission.setCode(request.getCode());
        submission.setLanguage(request.getLanguage().trim());
        submission.setResult(Enums.JudgeResult.PENDING.name());
        submission.setSubmitTime(LocalDateTime.now());
        submissionMapper.insert(submission);

        // TODO 接入 OJ 判题：投递判题队列，异步回写 result/timeUsed/memoryUsed。
        log.info("会员 {} 提交题目 {}，提交记录 {}",
                memberId, problemId, submission.getSubmitId());
        return ProblemDtos.SubmitVO.builder()
                .submitId(submission.getSubmitId())
                .result(submission.getResult())
                .build();
    }

    public List<ProblemDtos.SubmissionVO> submissions(Long problemId) {
        getOrThrow(problemId);
        List<Submission> submissions = submissionMapper.selectList(
                Wrappers.<Submission>lambdaQuery()
                        .eq(Submission::getProblemId, problemId)
                        .orderByDesc(Submission::getSubmitTime)
                        .orderByDesc(Submission::getSubmitId)
                        .last("LIMIT 50"));
        if (submissions.isEmpty()) {
            return List.of();
        }
        Set<Long> memberIds = submissions.stream()
                .map(Submission::getSubmitterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberIds.isEmpty()
                ? Map.of()
                : memberMapper.selectBatchIds(memberIds).stream()
                        .collect(Collectors.toMap(Member::getMemberId, member -> member));

        return submissions.stream()
                .map(submission -> {
                    Member submitter = memberMap.get(submission.getSubmitterId());
                    return ProblemDtos.SubmissionVO.builder()
                            .submitId(submission.getSubmitId())
                            .trainingId(submission.getTrainingId() == null
                                    ? 0L : submission.getTrainingId())
                            .problemId(submission.getProblemId())
                            .submitter(ProblemDtos.SubmitterVO.builder()
                                    .userId(submission.getSubmitterId())
                                    .studentId(submitter == null ? null : submitter.getStudentId())
                                    .name(submitter == null ? "未知会员" : submitter.getName())
                                    .roles(List.of())
                                    .build())
                            .language(submission.getLanguage())
                            .result(submission.getResult())
                            .timeUsed(nvl(submission.getTimeUsed()))
                            .memoryUsed(nvl(submission.getMemoryUsed()))
                            .submitTime(submission.getSubmitTime())
                            .build();
                })
                .toList();
    }

    // ==================== 内部工具 ====================

    private Problem getOrThrow(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw BusinessException.notFound("题目不存在");
        }
        return problem;
    }

    private Solution findSolution(Long problemId) {
        return solutionMapper.selectOne(Wrappers.<Solution>lambdaQuery()
                .eq(Solution::getProblemId, problemId));
    }

    private Map<Long, List<ProblemDtos.TagItem>> loadTags(Collection<Long> problemIds) {
        Set<Long> ids = problemIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ProblemRows.ProblemTagRow> rows = queryMapper.selectTagsByProblemIds(ids);
        return rows.stream().collect(Collectors.groupingBy(
                ProblemRows.ProblemTagRow::getProblemId,
                Collectors.mapping(row -> ProblemDtos.TagItem.builder()
                        .tagId(row.getTagId())
                        .name(row.getName())
                        .category(row.getCategory())
                        .build(), Collectors.toList())));
    }

    /**
     * 校验并去重知识点 ID。null 表示不修改，空列表表示清空关联。
     */
    private List<Long> distinctTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return List.of();
        }
        List<Long> ids = tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return ids;
        }
        Long count = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .in(Tag::getTagId, ids));
        if (count == null || count.longValue() != ids.size()) {
            throw BusinessException.badRequest("存在无效的知识点");
        }
        return ids;
    }

    private void saveTags(Long problemId, List<Long> tagIds) {
        problemTagMapper.delete(Wrappers.<ProblemTag>lambdaQuery()
                .eq(ProblemTag::getProblemId, problemId));
        for (Long tagId : tagIds) {
            ProblemTag link = new ProblemTag();
            link.setProblemId(problemId);
            link.setTagId(tagId);
            problemTagMapper.insert(link);
        }
    }

    private ProblemDtos.ProblemVO toVO(Problem problem, List<ProblemDtos.TagItem> tags,
                                       ProblemDtos.SolutionVO solution) {
        return ProblemDtos.ProblemVO.builder()
                .problemId(problem.getProblemId())
                .title(problem.getTitle())
                .ojProblemId(problem.getOjProblemId())
                .difficulty(problem.getDifficulty() == null
                        ? Enums.Difficulty.MEDIUM.name() : problem.getDifficulty())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .description(problem.getDescription())
                .inputFormat(problem.getInputFormat())
                .outputFormat(problem.getOutputFormat())
                .sampleInput(problem.getSampleInput())
                .sampleOutput(problem.getSampleOutput())
                .hint(problem.getHint())
                .tags(tags)
                .solution(solution)
                .visibility(problem.getVisibility() == null
                        ? Enums.Visibility.PUBLIC.name() : problem.getVisibility())
                .build();
    }

    private ProblemDtos.SolutionVO toSolutionVO(Solution solution) {
        return ProblemDtos.SolutionVO.builder()
                .solutionId(solution.getSolutionId())
                .content(solution.getContent())
                .referenceCode(solution.getReferenceCode())
                .updateTime(solution.getUpdateTime())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest(field + " 参数不合法：" + value);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
