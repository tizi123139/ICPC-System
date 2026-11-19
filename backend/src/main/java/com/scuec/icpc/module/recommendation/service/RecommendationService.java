package com.scuec.icpc.module.recommendation.service;

import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.recommendation.dto.RecommendationDtos;
import com.scuec.icpc.module.recommendation.dto.RecommendationRows;
import com.scuec.icpc.module.recommendation.mapper.RecommendationQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 个性化推荐：基于薄弱知识点推荐习题与训练。
 * 判题未接入时应答降级为“最新公开题目 / 最新训练”，不抛错。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int WEAK_TAG_LIMIT = 3;
    private static final int MAX_LIMIT = 50;

    private final RecommendationQueryMapper queryMapper;

    public List<RecommendationDtos.ProblemVO> recommendProblems(Long userId, Integer limit) {
        Long memberId = resolveTarget(userId);
        int size = normalizeLimit(limit, 10);

        List<RecommendationRows.WeakTagRow> weakTags =
                queryMapper.selectWeakTags(memberId, WEAK_TAG_LIMIT);
        if (weakTags.isEmpty()) {
            return queryMapper.selectLatestProblems(size).stream().map(this::toProblemVO).toList();
        }

        List<Long> tagIds = weakTags.stream()
                .map(RecommendationRows.WeakTagRow::getTagId).toList();
        List<RecommendationRows.ProblemRow> rows =
                queryMapper.selectRecommendedProblems(memberId, tagIds, size);
        if (rows.isEmpty()) {
            rows = queryMapper.selectLatestProblems(size);
        }
        return rows.stream().map(this::toProblemVO).toList();
    }

    public List<RecommendationDtos.TrainingVO> recommendTrainings(Long userId, Integer limit) {
        Long memberId = resolveTarget(userId);
        int size = normalizeLimit(limit, 5);

        List<RecommendationRows.WeakTagRow> weakTags =
                queryMapper.selectWeakTags(memberId, WEAK_TAG_LIMIT);
        if (weakTags.isEmpty()) {
            return queryMapper.selectLatestTrainings(size).stream().map(this::toTrainingVO).toList();
        }

        List<Long> tagIds = weakTags.stream()
                .map(RecommendationRows.WeakTagRow::getTagId).toList();
        List<RecommendationRows.TrainingRow> rows =
                queryMapper.selectRecommendedTrainings(tagIds, size);
        if (rows.isEmpty()) {
            rows = queryMapper.selectLatestTrainings(size);
        }
        return rows.stream().map(this::toTrainingVO).toList();
    }

    // ==================== 内部工具 ====================

    /**
     * userId 缺省为当前用户；查询他人需教练及以上。
     */
    private Long resolveTarget(Long userId) {
        Long memberId = userId == null ? SecurityUtils.currentMemberId() : userId;
        SecurityUtils.requireSelfOrAtLeast(memberId, Roles.ASSISTANT_COACH);
        return memberId;
    }

    private int normalizeLimit(Integer limit, int defaultSize) {
        if (limit == null || limit < 1) {
            return defaultSize;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private RecommendationDtos.ProblemVO toProblemVO(RecommendationRows.ProblemRow row) {
        return RecommendationDtos.ProblemVO.builder()
                .problemId(row.getProblemId())
                .title(row.getTitle())
                .ojProblemId(row.getOjProblemId())
                .difficulty(row.getDifficulty())
                .matchedTag(row.getTagName())
                .build();
    }

    private RecommendationDtos.TrainingVO toTrainingVO(RecommendationRows.TrainingRow row) {
        return RecommendationDtos.TrainingVO.builder()
                .trainingId(row.getTrainingId())
                .name(row.getName())
                .type(row.getType())
                .startTime(row.getStartTime())
                .endTime(row.getEndTime())
                .status(row.getStatus())
                .build();
    }
}
