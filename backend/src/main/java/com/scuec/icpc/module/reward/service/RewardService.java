package com.scuec.icpc.module.reward.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.reward.dto.RewardDtos;
import com.scuec.icpc.module.reward.entity.Punishment;
import com.scuec.icpc.module.reward.entity.Reward;
import com.scuec.icpc.module.reward.mapper.PunishmentMapper;
import com.scuec.icpc.module.reward.mapper.RewardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 奖惩管理业务：奖励与处分记录的查询、录入、修改与删除。
 * 普通会员仅能查看本人记录，干事及以上可查看全部并维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardMapper rewardMapper;
    private final PunishmentMapper punishmentMapper;
    private final MemberMapper memberMapper;

    // ==================== 奖励 ====================

    public List<RewardDtos.RewardVO> listRewards() {
        LambdaQueryWrapper<Reward> wrapper = Wrappers.<Reward>lambdaQuery();
        if (!SecurityUtils.hasAnyExactRole(Roles.OFFICER, Roles.PRESIDENT, Roles.ADVISOR)) {
            wrapper.eq(Reward::getMemberId, SecurityUtils.currentMemberId());
        }
        wrapper.orderByDesc(Reward::getGrantDate)
                .orderByDesc(Reward::getRewardId)
                .last("LIMIT 500");
        return toRewardVOs(rewardMapper.selectList(wrapper));
    }

    public RewardDtos.RewardVO rewardDetail(Long rewardId) {
        Reward reward = getRewardOrThrow(rewardId);
        if (!SecurityUtils.isSelf(reward.getMemberId())) {
            requireRewardManager();
        }
        return toRewardVOs(List.of(reward)).get(0);
    }

    @Transactional
    public RewardDtos.RewardVO createReward(RewardDtos.RewardRequest request) {
        requireRewardManager();
        Member member = getMemberOrThrow(request.getMemberId());
        Reward reward = new Reward();
        applyReward(reward, request);
        reward.setCreatorId(SecurityUtils.currentMemberId());
        rewardMapper.insert(reward);
        log.info("为会员 {} 新增奖励 {}", member.getMemberId(), reward.getTitle());
        return toRewardVO(reward, member);
    }

    @Transactional
    public RewardDtos.RewardVO updateReward(Long rewardId, RewardDtos.RewardRequest request) {
        requireRewardManager();
        Reward reward = getRewardOrThrow(rewardId);
        Member member = getMemberOrThrow(request.getMemberId());
        applyReward(reward, request);
        rewardMapper.updateById(reward);
        return toRewardVO(reward, member);
    }

    @Transactional
    public void deleteReward(Long rewardId) {
        requireRewardManager();
        getRewardOrThrow(rewardId);
        rewardMapper.deleteById(rewardId);
    }

    // ==================== 处分 ====================

    public List<RewardDtos.PunishmentVO> listPunishments() {
        LambdaQueryWrapper<Punishment> wrapper = Wrappers.<Punishment>lambdaQuery();
        if (!SecurityUtils.hasAnyExactRole(Roles.OFFICER, Roles.PRESIDENT, Roles.ADVISOR)) {
            wrapper.eq(Punishment::getMemberId, SecurityUtils.currentMemberId());
        }
        wrapper.orderByDesc(Punishment::getEffectiveDate)
                .orderByDesc(Punishment::getPunishmentId)
                .last("LIMIT 500");
        return toPunishmentVOs(punishmentMapper.selectList(wrapper));
    }

    public RewardDtos.PunishmentVO punishmentDetail(Long punishmentId) {
        Punishment punishment = getPunishmentOrThrow(punishmentId);
        if (!SecurityUtils.isSelf(punishment.getMemberId())) {
            requireRewardManager();
        }
        return toPunishmentVOs(List.of(punishment)).get(0);
    }

    @Transactional
    public RewardDtos.PunishmentVO createPunishment(RewardDtos.PunishmentRequest request) {
        requireRewardManager();
        Member member = getMemberOrThrow(request.getMemberId());
        Punishment punishment = new Punishment();
        applyPunishment(punishment, request);
        punishment.setCreatorId(SecurityUtils.currentMemberId());
        punishmentMapper.insert(punishment);
        log.info("为会员 {} 新增处分 {}", member.getMemberId(), punishment.getType());
        return toPunishmentVO(punishment, member);
    }

    @Transactional
    public RewardDtos.PunishmentVO updatePunishment(Long punishmentId,
                                                    RewardDtos.PunishmentRequest request) {
        requireRewardManager();
        Punishment punishment = getPunishmentOrThrow(punishmentId);
        Member member = getMemberOrThrow(request.getMemberId());
        applyPunishment(punishment, request);
        punishmentMapper.updateById(punishment);
        return toPunishmentVO(punishment, member);
    }

    @Transactional
    public void deletePunishment(Long punishmentId) {
        requireRewardManager();
        getPunishmentOrThrow(punishmentId);
        punishmentMapper.deleteById(punishmentId);
    }

    // ==================== 内部工具 ====================

    /**
     * 权限表：奖惩管理仅干事、会长/副会长、指导老师，助理教练不得维护。
     */
    private void requireRewardManager() {
        SecurityUtils.requireAnyExactRole("仅干事、会长/副会长或指导老师可维护奖惩记录",
                Roles.OFFICER, Roles.PRESIDENT, Roles.ADVISOR);
    }

    private void applyReward(Reward reward, RewardDtos.RewardRequest request) {
        reward.setMemberId(request.getMemberId());
        reward.setTitle(request.getTitle().trim());
        reward.setCategory(request.getCategory().trim());
        reward.setLevel(request.getLevel().trim());
        reward.setAmount(request.getAmount() == null ? BigDecimal.ZERO : request.getAmount());
        reward.setDescription(request.getDescription());
        reward.setGrantDate(request.getGrantDate());
        reward.setIssuerName(request.getIssuerName());
    }

    private void applyPunishment(Punishment punishment, RewardDtos.PunishmentRequest request) {
        punishment.setMemberId(request.getMemberId());
        punishment.setType(request.getType().trim());
        punishment.setReason(request.getReason().trim());
        punishment.setDescription(request.getDescription());
        punishment.setEffectiveDate(request.getEffectiveDate());
        punishment.setExpireDate(request.getExpireDate());
        punishment.setIssuerName(request.getIssuerName());
    }

    private Reward getRewardOrThrow(Long rewardId) {
        Reward reward = rewardMapper.selectById(rewardId);
        if (reward == null) {
            throw BusinessException.notFound("奖励记录不存在");
        }
        return reward;
    }

    private Punishment getPunishmentOrThrow(Long punishmentId) {
        Punishment punishment = punishmentMapper.selectById(punishmentId);
        if (punishment == null) {
            throw BusinessException.notFound("处分记录不存在");
        }
        return punishment;
    }

    private Member getMemberOrThrow(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.notFound("会员不存在");
        }
        return member;
    }

    private List<RewardDtos.RewardVO> toRewardVOs(List<Reward> rewards) {
        Map<Long, Member> memberMap = loadMemberMap(
                rewards.stream().map(Reward::getMemberId).toList());
        return rewards.stream().map(reward -> toRewardVO(reward,
                memberMap.get(reward.getMemberId()))).toList();
    }

    private List<RewardDtos.PunishmentVO> toPunishmentVOs(List<Punishment> punishments) {
        Map<Long, Member> memberMap = loadMemberMap(
                punishments.stream().map(Punishment::getMemberId).toList());
        return punishments.stream().map(punishment -> toPunishmentVO(punishment,
                memberMap.get(punishment.getMemberId()))).toList();
    }

    private RewardDtos.RewardVO toRewardVO(Reward reward, Member member) {
        return RewardDtos.RewardVO.builder()
                .rewardId(reward.getRewardId())
                .memberId(reward.getMemberId())
                .memberName(member == null ? null : member.getName())
                .studentId(member == null ? null : member.getStudentId())
                .title(reward.getTitle())
                .category(reward.getCategory())
                .level(reward.getLevel())
                .amount(reward.getAmount())
                .description(reward.getDescription())
                .grantDate(reward.getGrantDate())
                .issuerName(reward.getIssuerName())
                .createTime(reward.getCreateTime())
                .build();
    }

    private RewardDtos.PunishmentVO toPunishmentVO(Punishment punishment, Member member) {
        return RewardDtos.PunishmentVO.builder()
                .punishmentId(punishment.getPunishmentId())
                .memberId(punishment.getMemberId())
                .memberName(member == null ? null : member.getName())
                .studentId(member == null ? null : member.getStudentId())
                .type(punishment.getType())
                .reason(punishment.getReason())
                .description(punishment.getDescription())
                .effectiveDate(punishment.getEffectiveDate())
                .expireDate(punishment.getExpireDate())
                .issuerName(punishment.getIssuerName())
                .createTime(punishment.getCreateTime())
                .build();
    }

    private Map<Long, Member> loadMemberMap(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Member::getMemberId, Function.identity(), (a, b) -> a));
    }
}
