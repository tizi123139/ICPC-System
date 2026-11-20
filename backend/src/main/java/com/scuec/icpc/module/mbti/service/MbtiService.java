package com.scuec.icpc.module.mbti.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.mbti.dto.MbtiDtos;
import com.scuec.icpc.module.mbti.entity.MbtiQuestion;
import com.scuec.icpc.module.mbti.entity.MbtiResult;
import com.scuec.icpc.module.mbti.mapper.MbtiQuestionMapper;
import com.scuec.icpc.module.mbti.mapper.MbtiResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MBTI 测评业务：题目下发、答案计分、结果查询。
 * 题库中 dimension 以 E_I / S_N / T_F / J_P 存储，下发时拆成单字母方向。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MbtiService {

    /**
     * 维度顺序固定，用于拼接四字母类型。
     */
    private static final List<String> DIMENSIONS = List.of("E_I", "S_N", "T_F", "J_P");

    private static final Map<String, String[]> DIMENSION_LETTERS = Map.of(
            "E_I", new String[]{"E", "I"},
            "S_N", new String[]{"S", "N"},
            "T_F", new String[]{"T", "F"},
            "J_P", new String[]{"J", "P"});

    private final MbtiQuestionMapper questionMapper;
    private final MbtiResultMapper resultMapper;

    // ==================== 题目 ====================

    public List<MbtiDtos.QuestionVO> questions() {
        // 权限表：MBTI 仅观察会员、正式会员、队长、助理教练、会长/副会长参与。
        SecurityUtils.requireAnyExactRole("当前角色不参与 MBTI 测评",
                Roles.OBSERVE_MEMBER, Roles.MEMBER, Roles.TEAM_LEADER,
                Roles.ASSISTANT_COACH, Roles.PRESIDENT);
        return questionMapper.selectList(Wrappers.<MbtiQuestion>lambdaQuery()
                        .orderByAsc(MbtiQuestion::getQuestionNo)
                        .orderByAsc(MbtiQuestion::getQuestionId))
                .stream()
                .map(this::toQuestionVO)
                .toList();
    }

    private MbtiDtos.QuestionVO toQuestionVO(MbtiQuestion question) {
        String[] letters = lettersOf(question.getDimension());
        return MbtiDtos.QuestionVO.builder()
                .questionId(question.getQuestionId())
                .content(question.getContent())
                .options(List.of(
                        MbtiDtos.OptionVO.builder()
                                .option("A")
                                .dimension(letters[0])
                                .value(nvl(question.getScoreA()))
                                .build(),
                        MbtiDtos.OptionVO.builder()
                                .option("B")
                                .dimension(letters[1])
                                .value(nvl(question.getScoreB()))
                                .build()))
                .build();
    }

    // ==================== 提交与结果 ====================

    @Transactional
    public MbtiDtos.ResultVO submit(MbtiDtos.SubmitRequest request) {
        SecurityUtils.requireAnyExactRole("当前角色不参与 MBTI 测评",
                Roles.OBSERVE_MEMBER, Roles.MEMBER, Roles.TEAM_LEADER,
                Roles.ASSISTANT_COACH, Roles.PRESIDENT);
        Long memberId = SecurityUtils.currentMemberId();
        List<MbtiQuestion> all = questionMapper.selectList(null);
        if (all.isEmpty()) {
            throw BusinessException.conflict("题库暂未配置 MBTI 题目");
        }
        Map<Long, MbtiQuestion> questionMap = all.stream()
                .collect(Collectors.toMap(MbtiQuestion::getQuestionId, question -> question));

        Map<String, int[]> scoreMap = new LinkedHashMap<>();
        DIMENSIONS.forEach(dimension -> scoreMap.put(dimension, new int[2]));

        Set<Long> answered = new HashSet<>();
        for (MbtiDtos.Answer answer : request.getAnswers()) {
            MbtiQuestion question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                throw BusinessException.badRequest("题目不存在：" + answer.getQuestionId());
            }
            if (!answered.add(answer.getQuestionId())) {
                throw BusinessException.badRequest("题目重复作答：" + answer.getQuestionId());
            }
            // 提交前校验维度配置，避免脏数据导致类型拼接异常
            lettersOf(question.getDimension());
            int[] scores = scoreMap.computeIfAbsent(question.getDimension(), key -> new int[2]);
            if ("A".equalsIgnoreCase(answer.getOption())) {
                scores[0] += nvl(question.getScoreA());
            } else {
                scores[1] += nvl(question.getScoreB());
            }
        }

        StringBuilder type = new StringBuilder(4);
        Map<String, Double> resultScores = new LinkedHashMap<>();
        for (String dimension : DIMENSIONS) {
            String[] letters = lettersOf(dimension);
            int[] scores = scoreMap.getOrDefault(dimension, new int[]{0, 0});
            int total = scores[0] + scores[1];
            // 未作答的维度按 50% 处理，取该维度第一个字母
            double percentA = total == 0 ? 50.0
                    : Math.round(scores[0] * 1000.0 / total) / 10.0;
            type.append(percentA >= 50.0 ? letters[0] : letters[1]);
            resultScores.put(dimension, percentA);
        }

        String mbtiType = type.toString();
        MbtiResult result = new MbtiResult();
        result.setMemberId(memberId);
        result.setType(mbtiType);
        result.setScoreEi(resultScores.get("E_I"));
        result.setScoreSn(resultScores.get("S_N"));
        result.setScoreTf(resultScores.get("T_F"));
        result.setScoreJp(resultScores.get("J_P"));
        result.setReport(buildReport(mbtiType, resultScores));
        result.setTestTime(LocalDateTime.now());
        resultMapper.insert(result);

        log.info("会员 {} 完成 MBTI 测评，类型 {}", memberId, mbtiType);
        return toVO(result);
    }

    public MbtiDtos.ResultVO latest(Long memberId) {
        SecurityUtils.requireAnyExactRole("当前角色不参与 MBTI 测评",
                Roles.OBSERVE_MEMBER, Roles.MEMBER, Roles.TEAM_LEADER,
                Roles.ASSISTANT_COACH, Roles.PRESIDENT);
        SecurityUtils.requireSelfOrAtLeast(memberId, Roles.ASSISTANT_COACH);
        MbtiResult result = resultMapper.selectOne(Wrappers.<MbtiResult>lambdaQuery()
                .eq(MbtiResult::getMemberId, memberId)
                .orderByDesc(MbtiResult::getTestTime)
                .orderByDesc(MbtiResult::getResultId)
                .last("LIMIT 1"));
        return result == null ? null : toVO(result);
    }

    private MbtiDtos.ResultVO toVO(MbtiResult result) {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("E_I", result.getScoreEi());
        scores.put("S_N", result.getScoreSn());
        scores.put("T_F", result.getScoreTf());
        scores.put("J_P", result.getScoreJp());
        return MbtiDtos.ResultVO.builder()
                .resultId(result.getResultId())
                .userId(result.getMemberId())
                .type(result.getType())
                .scores(scores)
                .report(result.getReport())
                .reportUrl(null)
                .suggestions(suggestionsFor(result.getType()))
                .testTime(result.getTestTime())
                .build();
    }

    /**
     * 根据 MBTI 四字母类型生成发展建议，保证结果页“发展建议”始终有内容。
     */
    private List<String> suggestionsFor(String type) {
        if (type == null || type.length() != 4) {
            return List.of();
        }
        char energy = type.charAt(0);
        char perception = type.charAt(1);
        char decision = type.charAt(2);
        char lifestyle = type.charAt(3);
        return List.of(
                energy == 'E'
                        ? "多参与团队讨论，主动表达自己的思路，带动队伍氛围"
                        : "保留独立思考时间，同时定期向队友同步进展，减少信息差",
                perception == 'S'
                        ? "继续发挥注重细节和实操的优势，同时关注整体解题节奏"
                        : "大胆提出创新解法，同时补充可行性验证和边界测试",
                decision == 'T'
                        ? "决策时兼顾队友感受，注意沟通方式，避免过于直接"
                        : "在关注团队氛围的同时，补充客观数据和复盘结论支撑决策",
                lifestyle == 'J'
                        ? "保持计划性的同时，为临场调整预留空间"
                        : "发挥灵活应变的优势，同时为关键节点设置检查点");
    }

    // ==================== 内部工具 ====================

    private String buildReport(String type, Map<String, Double> scores) {
        StringBuilder builder = new StringBuilder();
        builder.append("你的 MBTI 类型为 ").append(type).append("。\n");
        builder.append("各维度倾向：\n");
        for (String dimension : DIMENSIONS) {
            String[] letters = lettersOf(dimension);
            double percentA = scores.getOrDefault(dimension, 50.0);
            boolean firstSide = percentA >= 50.0;
            String dominant = firstSide ? letters[0] : letters[1];
            double percent = firstSide ? percentA : 100.0 - percentA;
            builder.append("  ").append(letters[0]).append('/').append(letters[1])
                    .append(" 偏向 ").append(dominant)
                    .append(String.format(Locale.ROOT, " %.1f%%", percent))
                    .append('\n');
        }
        builder.append("说明：E/I 外向-内向，S/N 实感-直觉，T/F 思考-情感，J/P 判断-知觉。");
        return builder.toString();
    }

    private String[] lettersOf(String dimension) {
        String[] letters = DIMENSION_LETTERS.get(dimension);
        if (letters == null) {
            throw BusinessException.badRequest("题库维度配置异常：" + dimension);
        }
        return letters;
    }

    private int nvl(Integer value) {
        return value == null ? 1 : value;
    }
}
