package com.scuec.icpc.module.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.assessment.dto.AssessmentDtos;
import com.scuec.icpc.module.assessment.entity.Assessment;
import com.scuec.icpc.module.assessment.entity.AssessmentResult;
import com.scuec.icpc.module.assessment.mapper.AssessmentMapper;
import com.scuec.icpc.module.assessment.mapper.AssessmentResultMapper;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考核管理业务：考核创建/发布、成绩录入与审核。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private static final Set<String> TYPES = Set.of("OBSERVATION", "REGULAR", "PROMOTION");
    private static final Set<String> AUDIT_STATUS = Set.of(APPROVED, REJECTED);

    private final AssessmentMapper assessmentMapper;
    private final AssessmentResultMapper resultMapper;
    private final MemberMapper memberMapper;

    // ==================== 考核 ====================

    public List<AssessmentDtos.AssessmentVO> list() {
        LambdaQueryWrapper<Assessment> wrapper = Wrappers.<Assessment>lambdaQuery();
        // 普通会员仅能看到已发布的考核
        if (!SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH)) {
            wrapper.eq(Assessment::getStatus, PUBLISHED);
        }
        wrapper.orderByDesc(Assessment::getCreateTime)
                .orderByDesc(Assessment::getAssessmentId);
        return assessmentMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public AssessmentDtos.AssessmentVO detail(Long assessmentId) {
        Assessment assessment = getOrThrow(assessmentId);
        if (!PUBLISHED.equals(assessment.getStatus())
                && !SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH)) {
            throw BusinessException.notFound("考核不存在或尚未发布");
        }
        return toVO(assessment);
    }

    @Transactional
    public AssessmentDtos.AssessmentVO create(AssessmentDtos.CreateRequest request) {
        Assessment assessment = new Assessment();
        assessment.setName(request.getName().trim());
        assessment.setType(parseType(request.getType()));
        assessment.setDescription(request.getDescription());
        assessment.setStartTime(request.getStartTime());
        assessment.setEndTime(request.getEndTime());
        assessment.setPassScore(request.getPassScore() == null ? 60 : request.getPassScore());
        assessment.setStatus(DRAFT);
        assessment.setCreatorId(SecurityUtils.currentMemberId());
        assessmentMapper.insert(assessment);
        return toVO(assessment);
    }

    @Transactional
    public AssessmentDtos.AssessmentVO update(Long assessmentId,
                                             AssessmentDtos.UpdateRequest request) {
        Assessment assessment = getOrThrow(assessmentId);
        if (!DRAFT.equals(assessment.getStatus())) {
            throw BusinessException.conflict("仅草稿状态的考核可以修改");
        }
        if (StringUtils.hasText(request.getName())) {
            assessment.setName(request.getName().trim());
        }
        if (StringUtils.hasText(request.getType())) {
            assessment.setType(parseType(request.getType()));
        }
        if (request.getDescription() != null) {
            assessment.setDescription(request.getDescription());
        }
        if (request.getStartTime() != null) {
            assessment.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            assessment.setEndTime(request.getEndTime());
        }
        if (request.getPassScore() != null) {
            assessment.setPassScore(request.getPassScore());
        }
        assessmentMapper.updateById(assessment);
        return toVO(assessment);
    }

    @Transactional
    public void delete(Long assessmentId) {
        getOrThrow(assessmentId);
        resultMapper.delete(Wrappers.<AssessmentResult>lambdaQuery()
                .eq(AssessmentResult::getAssessmentId, assessmentId));
        assessmentMapper.deleteById(assessmentId);
    }

    @Transactional
    public void publish(Long assessmentId) {
        Assessment assessment = getOrThrow(assessmentId);
        if (PUBLISHED.equals(assessment.getStatus())) {
            return;
        }
        assessment.setStatus(PUBLISHED);
        assessmentMapper.updateById(assessment);
        log.info("考核 {} 已发布", assessmentId);
    }

    // ==================== 成绩 ====================

    public List<AssessmentDtos.ResultVO> results(Long assessmentId) {
        getOrThrow(assessmentId);
        LambdaQueryWrapper<AssessmentResult> wrapper = Wrappers.<AssessmentResult>lambdaQuery()
                .eq(AssessmentResult::getAssessmentId, assessmentId);
        if (!SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH)) {
            wrapper.eq(AssessmentResult::getMemberId, SecurityUtils.currentMemberId());
        }
        wrapper.orderByDesc(AssessmentResult::getScore)
                .orderByAsc(AssessmentResult::getResultId);
        return toResultVOs(resultMapper.selectList(wrapper));
    }

    @Transactional
    public void updateResults(Long assessmentId, AssessmentDtos.ResultsRequest request) {
        Assessment assessment = getOrThrow(assessmentId);
        for (AssessmentDtos.ResultItem item : request.getResults()) {
            Member member = memberMapper.selectById(item.getMemberId());
            if (member == null) {
                throw BusinessException.notFound("会员不存在：" + item.getMemberId());
            }
            AssessmentResult result = resultMapper.selectOne(Wrappers.<AssessmentResult>lambdaQuery()
                    .eq(AssessmentResult::getAssessmentId, assessmentId)
                    .eq(AssessmentResult::getMemberId, item.getMemberId()));
            if (result == null) {
                result = new AssessmentResult();
                result.setAssessmentId(assessmentId);
                result.setMemberId(item.getMemberId());
                result.setAuditStatus(PENDING);
                result.setScore(item.getScore());
                result.setRemark(item.getRemark());
                resultMapper.insert(result);
            } else {
                result.setScore(item.getScore());
                result.setRemark(item.getRemark());
                result.setAuditStatus(PENDING);
                result.setAuditRemark(null);
                result.setAuditorId(null);
                result.setAuditTime(null);
                resultMapper.updateById(result);
            }
        }
        log.info("考核 {} 录入 {} 条成绩", assessment.getAssessmentId(),
                request.getResults().size());
    }

    @Transactional
    public void auditResult(Long assessmentId, Long resultId,
                            AssessmentDtos.AuditRequest request) {
        AssessmentResult result = resultMapper.selectById(resultId);
        if (result == null || !assessmentId.equals(result.getAssessmentId())) {
            throw BusinessException.notFound("考核成绩不存在");
        }
        String status = request.getStatus() == null ? "" : request.getStatus().trim();
        if (!AUDIT_STATUS.contains(status)) {
            throw BusinessException.badRequest("审核状态取值不合法：" + request.getStatus());
        }
        result.setAuditStatus(status);
        result.setAuditRemark(request.getAuditRemark());
        result.setAuditorId(SecurityUtils.currentMemberId());
        result.setAuditTime(LocalDateTime.now());
        resultMapper.updateById(result);
    }

    /**
     * 查询某会员的全部考核记录（会员档案 / 会员详情使用）。
     */
    public List<AssessmentDtos.ResultVO> memberResults(Long memberId) {
        SecurityUtils.requireSelfOrAtLeast(memberId, Roles.ASSISTANT_COACH);
        List<AssessmentResult> results = resultMapper.selectList(
                Wrappers.<AssessmentResult>lambdaQuery()
                        .eq(AssessmentResult::getMemberId, memberId)
                        .orderByDesc(AssessmentResult::getResultId));
        return toResultVOs(results);
    }

    // ==================== 内部工具 ====================

    private Assessment getOrThrow(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw BusinessException.notFound("考核不存在");
        }
        return assessment;
    }

    private String parseType(String type) {
        if (!TYPES.contains(type)) {
            throw BusinessException.badRequest("考核类型取值不合法：" + type);
        }
        return type;
    }

    private AssessmentDtos.AssessmentVO toVO(Assessment assessment) {
        return AssessmentDtos.AssessmentVO.builder()
                .assessmentId(assessment.getAssessmentId())
                .name(assessment.getName())
                .type(assessment.getType())
                .description(assessment.getDescription())
                .startTime(assessment.getStartTime())
                .endTime(assessment.getEndTime())
                .passScore(assessment.getPassScore())
                .status(assessment.getStatus())
                .createTime(assessment.getCreateTime())
                .build();
    }

    private List<AssessmentDtos.ResultVO> toResultVOs(List<AssessmentResult> results) {
        Map<Long, Member> memberMap = loadMemberMap(
                results.stream().map(AssessmentResult::getMemberId).toList());
        Map<Long, String> assessmentNames = loadAssessmentNames(
                results.stream().map(AssessmentResult::getAssessmentId).toList());
        return results.stream().map(result -> {
            Member member = memberMap.get(result.getMemberId());
            return AssessmentDtos.ResultVO.builder()
                    .resultId(result.getResultId())
                    .assessmentId(result.getAssessmentId())
                    .assessmentName(assessmentNames.get(result.getAssessmentId()))
                    .memberId(result.getMemberId())
                    .memberName(member == null ? null : member.getName())
                    .studentId(member == null ? null : member.getStudentId())
                    .score(result.getScore())
                    .remark(result.getRemark())
                    .auditStatus(result.getAuditStatus())
                    .auditRemark(result.getAuditRemark())
                    .build();
        }).toList();
    }

    private Map<Long, Member> loadMemberMap(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Member::getMemberId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> loadAssessmentNames(Collection<Long> assessmentIds) {
        List<Long> ids = assessmentIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return assessmentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Assessment::getAssessmentId, Assessment::getName, (a, b) -> a));
    }
}
