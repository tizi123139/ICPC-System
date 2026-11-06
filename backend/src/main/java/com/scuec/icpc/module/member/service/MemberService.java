package com.scuec.icpc.module.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scuec.icpc.common.api.PageResult;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.enums.Enums;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.member.dto.MemberDtos;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.entity.MemberApply;
import com.scuec.icpc.module.member.entity.MemberRole;
import com.scuec.icpc.module.member.mapper.MemberApplyMapper;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.member.mapper.MemberRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会员管理业务：会员 CRUD、入会/退会申请、审批与会员档案。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    /**
     * 管理员新增会员、审批入会时的初始密码。首次登录后应提示修改。
     */
    public static final String DEFAULT_PASSWORD = "123456";

    private static final List<String> BASE_ROLES =
            List.of(Roles.OBSERVE_MEMBER, Roles.MEMBER, Roles.LAB_MEMBER);

    private final MemberMapper memberMapper;
    private final MemberApplyMapper memberApplyMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final PasswordEncoder passwordEncoder;

    // ==================== 会员信息 ====================

    public PageResult<MemberDtos.MemberVO> page(MemberDtos.Query query) {
        LambdaQueryWrapper<Member> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Member::getName, keyword)
                    .or()
                    .like(Member::getStudentId, keyword));
        }
        if (StringUtils.hasText(query.getMemberType())) {
            wrapper.eq(Member::getMemberType, parseEnum(Enums.MemberType.class,
                    query.getMemberType(), "memberType").name());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Member::getStatus, parseEnum(Enums.MemberStatus.class,
                    query.getStatus(), "status").name());
        }
        if (StringUtils.hasText(query.getGrade())) {
            wrapper.eq(Member::getGrade, query.getGrade().trim());
        }
        if (StringUtils.hasText(query.getDepartment())) {
            wrapper.eq(Member::getDepartment, query.getDepartment().trim());
        }
        wrapper.orderByDesc(Member::getCreateTime).orderByDesc(Member::getMemberId);

        Page<Member> page = memberMapper.selectPage(query.toPage(), wrapper);
        Map<Long, List<String>> roleMap = loadRoleMap(
                page.getRecords().stream().map(Member::getMemberId).toList());
        return PageResult.of(page, member -> toVO(member, roleMap));
    }

    public MemberDtos.MemberVO detail(Long memberId) {
        Member member = getOrThrow(memberId);
        return toVO(member, Map.of(memberId, memberRoleMapper.selectRoleCodes(memberId)));
    }

    @Transactional
    public MemberDtos.MemberVO create(MemberDtos.CreateRequest request) {
        Long exists = memberMapper.selectCount(Wrappers.<Member>lambdaQuery()
                .eq(Member::getStudentId, request.getStudentId().trim()));
        if (exists != null && exists > 0) {
            throw BusinessException.conflict("该学号已存在会员记录");
        }
        String memberType = parseEnum(Enums.MemberType.class,
                request.getMemberType(), "memberType").name();

        Member member = new Member();
        member.setStudentId(request.getStudentId().trim());
        member.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        member.setName(request.getName().trim());
        member.setMajor(request.getMajor());
        member.setGrade(request.getGrade());
        member.setPhone(request.getPhone());
        member.setEmail(request.getEmail());
        member.setMemberType(memberType);
        member.setDepartment(request.getDepartment());
        member.setJoinDate(request.getJoinDate() == null ? LocalDate.now() : request.getJoinDate());
        member.setStatus(Enums.MemberStatus.ACTIVE.name());
        memberMapper.insert(member);
        addRole(member.getMemberId(), baseRoleOf(memberType));
        return toVO(member, Map.of(member.getMemberId(),
                List.of(baseRoleOf(memberType))));
    }

    @Transactional
    public MemberDtos.MemberVO update(Long memberId, MemberDtos.UpdateRequest request) {
        Member member = getOrThrow(memberId);
        boolean manager = SecurityUtils.isAtLeast(Roles.OFFICER);
        if (!manager && !SecurityUtils.isSelf(memberId)) {
            throw BusinessException.forbidden("只能修改自己的资料");
        }
        if (!manager && (StringUtils.hasText(request.getMemberType())
                || StringUtils.hasText(request.getStatus())
                || StringUtils.hasText(request.getDepartment()))) {
            throw BusinessException.forbidden("会员类型、状态与部门仅管理员可修改");
        }

        if (StringUtils.hasText(request.getName())) {
            member.setName(request.getName().trim());
        }
        if (request.getMajor() != null) {
            member.setMajor(request.getMajor());
        }
        if (request.getGrade() != null) {
            member.setGrade(request.getGrade());
        }
        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            member.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            member.setAvatar(request.getAvatar());
        }
        if (manager && StringUtils.hasText(request.getDepartment())) {
            member.setDepartment(request.getDepartment());
        }
        if (manager && StringUtils.hasText(request.getStatus())) {
            member.setStatus(parseEnum(Enums.MemberStatus.class,
                    request.getStatus(), "status").name());
        }
        if (manager && StringUtils.hasText(request.getMemberType())) {
            String memberType = parseEnum(Enums.MemberType.class,
                    request.getMemberType(), "memberType").name();
            if (!memberType.equals(member.getMemberType())) {
                member.setMemberType(memberType);
                syncBaseRole(memberId, memberType);
            }
        }
        memberMapper.updateById(member);
        return toVO(member, Map.of(memberId, memberRoleMapper.selectRoleCodes(memberId)));
    }

    @Transactional
    public void delete(Long memberId) {
        Member member = getOrThrow(memberId);
        if (!SecurityUtils.isAtLeast(Roles.PRESIDENT)) {
            throw BusinessException.forbidden("仅会长及以上角色可删除会员");
        }
        if (Enums.MemberStatus.REMOVED.name().equals(member.getStatus())) {
            return;
        }
        member.setStatus(Enums.MemberStatus.REMOVED.name());
        memberMapper.updateById(member);
        memberMapper.deleteById(memberId);
        memberRoleMapper.delete(Wrappers.<MemberRole>lambdaQuery()
                .eq(MemberRole::getMemberId, memberId));
    }

    public MemberDtos.ArchiveVO archive(Long memberId) {
        SecurityUtils.requireSelfOrAtLeast(memberId, Roles.OFFICER);
        Member member = getOrThrow(memberId);
        MemberDtos.MemberVO basicInfo = toVO(member,
                Map.of(memberId, memberRoleMapper.selectRoleCodes(memberId)));
        return MemberDtos.ArchiveVO.builder()
                .basicInfo(basicInfo)
                .trainingRecords(memberMapper.selectTrainingRecords(memberId))
                .contestRecords(memberMapper.selectContestRecords(memberId))
                // 奖惩表尚未在 M1 数据库设计中落地，先返回空列表，后续接入奖惩模块。
                .rewards(Collections.emptyList())
                .punishments(Collections.emptyList())
                .build();
    }

    // ==================== 入会 / 退会申请 ====================

    @Transactional
    public void applyJoin(MemberDtos.ApplyJoinRequest request) {
        String studentId = request.getStudentId().trim();
        Long exists = memberMapper.selectCount(Wrappers.<Member>lambdaQuery()
                .eq(Member::getStudentId, studentId));
        if (exists != null && exists > 0) {
            throw BusinessException.conflict("该学号已是协会会员，无需重复申请");
        }
        boolean duplicated = pendingJoinApplications().stream()
                .anyMatch(apply -> studentId.equals(
                        stringValue(apply.getApplyData(), "studentId")));
        if (duplicated) {
            throw BusinessException.conflict("该学号已有待审批的入会申请");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", studentId);
        data.put("name", request.getName().trim());
        data.put("major", request.getMajor());
        data.put("grade", request.getGrade());
        data.put("phone", request.getPhone());
        data.put("email", request.getEmail());
        data.put("reason", request.getReason());

        MemberApply apply = new MemberApply();
        apply.setType(Enums.ApplyType.JOIN_MEMBER.name());
        apply.setApplyData(data);
        apply.setStatus(Enums.ApplyStatus.PENDING.name());
        apply.setApplyTime(LocalDateTime.now());
        memberApplyMapper.insert(apply);
    }

    @Transactional
    public void applyResign(MemberDtos.ApplyResignRequest request) {
        Long memberId = SecurityUtils.currentMemberId();
        Member member = getOrThrow(memberId);
        if (!Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.conflict("当前会员状态不允许提交退会申请");
        }
        Long pending = memberApplyMapper.selectCount(Wrappers.<MemberApply>lambdaQuery()
                .eq(MemberApply::getApplicantId, memberId)
                .eq(MemberApply::getType, Enums.ApplyType.RESIGN.name())
                .eq(MemberApply::getStatus, Enums.ApplyStatus.PENDING.name()));
        if (pending != null && pending > 0) {
            throw BusinessException.conflict("已有待审批的退会申请，请勿重复提交");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reason", request.getReason());

        MemberApply apply = new MemberApply();
        apply.setType(Enums.ApplyType.RESIGN.name());
        apply.setApplicantId(memberId);
        apply.setApplyData(data);
        apply.setStatus(Enums.ApplyStatus.PENDING.name());
        apply.setApplyTime(LocalDateTime.now());
        memberApplyMapper.insert(apply);
    }

    public List<MemberDtos.ApplyVO> listApprovals(MemberDtos.ApplyQuery query) {
        LambdaQueryWrapper<MemberApply> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(MemberApply::getType,
                    parseEnum(Enums.ApplyType.class, query.getType(), "type").name());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(MemberApply::getStatus,
                    parseEnum(Enums.ApplyStatus.class, query.getStatus(), "status").name());
        }
        wrapper.orderByDesc(MemberApply::getApplyTime)
                .orderByDesc(MemberApply::getApplyId)
                .last("LIMIT 200");
        List<MemberApply> applies = memberApplyMapper.selectList(wrapper);
        if (applies.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = new ArrayList<>();
        for (MemberApply apply : applies) {
            if (apply.getApplicantId() != null) {
                memberIds.add(apply.getApplicantId());
            }
            if (apply.getAuditorId() != null) {
                memberIds.add(apply.getAuditorId());
            }
        }
        Map<Long, String> nameMap = loadMemberNames(memberIds);
        return applies.stream().map(apply -> toApplyVO(apply, nameMap)).toList();
    }

    @Transactional
    public void audit(Long applyId, MemberDtos.AuditRequest request) {
        MemberApply apply = memberApplyMapper.selectById(applyId);
        if (apply == null) {
            throw BusinessException.notFound("申请记录不存在");
        }
        if (!Enums.ApplyStatus.PENDING.name().equals(apply.getStatus())) {
            throw BusinessException.conflict("该申请已处理，不能重复审批");
        }
        Enums.AuditAction action = parseEnum(Enums.AuditAction.class,
                request.getAction(), "action");

        // 入会审批由干事及以上负责，退会/除名审批由部长及以上负责。
        if (Enums.ApplyType.JOIN_MEMBER.name().equals(apply.getType())) {
            if (!SecurityUtils.isAtLeast(Roles.OFFICER)) {
                throw BusinessException.forbidden("仅干事及以上角色可审批入会申请");
            }
        } else if (Enums.ApplyType.RESIGN.name().equals(apply.getType())) {
            if (!SecurityUtils.isAtLeast(Roles.MINISTER)) {
                throw BusinessException.forbidden("仅部长及以上角色可审批退会申请");
            }
        } else {
            throw BusinessException.badRequest("暂不支持该类型的申请审批");
        }

        if (action == Enums.AuditAction.APPROVE) {
            if (Enums.ApplyType.JOIN_MEMBER.name().equals(apply.getType())) {
                approveJoin(apply);
            } else {
                approveResign(apply);
            }
        }

        apply.setStatus(action == Enums.AuditAction.APPROVE
                ? Enums.ApplyStatus.APPROVED.name()
                : Enums.ApplyStatus.REJECTED.name());
        apply.setAuditRemark(request.getRemark());
        apply.setAuditorId(SecurityUtils.currentMemberId());
        apply.setAuditTime(LocalDateTime.now());
        memberApplyMapper.updateById(apply);
    }

    private void approveJoin(MemberApply apply) {
        Map<String, Object> data = apply.getApplyData() == null
                ? Collections.emptyMap() : apply.getApplyData();
        String studentId = stringValue(data, "studentId");
        String name = stringValue(data, "name");
        if (!StringUtils.hasText(studentId) || !StringUtils.hasText(name)) {
            throw BusinessException.badRequest("入会申请数据不完整，缺少学号或姓名");
        }
        Long exists = memberMapper.selectCount(Wrappers.<Member>lambdaQuery()
                .eq(Member::getStudentId, studentId));
        if (exists != null && exists > 0) {
            throw BusinessException.conflict("该学号已存在会员记录，无法重复创建");
        }

        Member member = new Member();
        member.setStudentId(studentId);
        member.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        member.setName(name);
        member.setMajor(stringValue(data, "major"));
        member.setGrade(stringValue(data, "grade"));
        member.setPhone(stringValue(data, "phone"));
        member.setEmail(stringValue(data, "email"));
        member.setMemberType(Enums.MemberType.OBSERVE_MEMBER.name());
        member.setJoinDate(LocalDate.now());
        member.setStatus(Enums.MemberStatus.ACTIVE.name());
        memberMapper.insert(member);
        addRole(member.getMemberId(), Roles.OBSERVE_MEMBER);
        // 回填申请人，便于后续按会员查询申请记录。
        apply.setApplicantId(member.getMemberId());
        log.info("入会申请 {} 审批通过，创建会员 {}（{}）",
                apply.getApplyId(), member.getMemberId(), member.getStudentId());
    }

    private void approveResign(MemberApply apply) {
        if (apply.getApplicantId() == null) {
            throw BusinessException.badRequest("退会申请缺少申请人信息");
        }
        Member member = getOrThrow(apply.getApplicantId());
        member.setStatus(Enums.MemberStatus.RESIGNED.name());
        memberMapper.updateById(member);
        // 退会后移除全部角色，避免旧 Token 在有效期内继续访问受保护接口。
        memberRoleMapper.delete(Wrappers.<MemberRole>lambdaQuery()
                .eq(MemberRole::getMemberId, member.getMemberId()));
        log.info("退会申请 {} 审批通过，会员 {} 状态置为 RESIGNED",
                apply.getApplyId(), member.getMemberId());
    }

    private List<MemberApply> pendingJoinApplications() {
        return memberApplyMapper.selectList(Wrappers.<MemberApply>lambdaQuery()
                .eq(MemberApply::getType, Enums.ApplyType.JOIN_MEMBER.name())
                .eq(MemberApply::getStatus, Enums.ApplyStatus.PENDING.name()));
    }

    // ==================== 内部工具 ====================

    private Member getOrThrow(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.notFound("会员不存在");
        }
        return member;
    }

    private MemberDtos.MemberVO toVO(Member member, Map<Long, List<String>> roleMap) {
        return MemberDtos.MemberVO.builder()
                .memberId(member.getMemberId())
                .studentId(member.getStudentId())
                .name(member.getName())
                .major(member.getMajor())
                .grade(member.getGrade())
                .phone(member.getPhone())
                .email(member.getEmail())
                .avatar(member.getAvatar())
                .memberType(member.getMemberType())
                .department(member.getDepartment())
                .joinDate(member.getJoinDate())
                .status(member.getStatus())
                .roles(roleMap.getOrDefault(member.getMemberId(), List.of()))
                .createTime(member.getCreateTime())
                .updateTime(member.getUpdateTime())
                .build();
    }

    private MemberDtos.ApplyVO toApplyVO(MemberApply apply, Map<Long, String> nameMap) {
        String applicantName = apply.getApplicantId() == null
                ? stringValue(apply.getApplyData(), "name")
                : nameMap.get(apply.getApplicantId());
        return MemberDtos.ApplyVO.builder()
                .applyId(apply.getApplyId())
                .type(apply.getType())
                .applicantId(apply.getApplicantId())
                .applicantName(applicantName)
                .applyData(apply.getApplyData())
                .status(apply.getStatus())
                .auditRemark(apply.getAuditRemark())
                .auditorId(apply.getAuditorId())
                .auditorName(apply.getAuditorId() == null ? null : nameMap.get(apply.getAuditorId()))
                .applyTime(apply.getApplyTime())
                .auditTime(apply.getAuditTime())
                .build();
    }

    private Map<Long, List<String>> loadRoleMap(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        List<MemberRole> roles = memberRoleMapper.selectList(Wrappers.<MemberRole>lambdaQuery()
                .in(MemberRole::getMemberId, memberIds));
        return roles.stream().collect(Collectors.groupingBy(
                MemberRole::getMemberId,
                Collectors.mapping(MemberRole::getRoleCode, Collectors.toList())));
    }

    private Map<Long, String> loadMemberNames(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return memberMapper.selectBatchIds(memberIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Member::getMemberId, Member::getName, (a, b) -> a));
    }

    private void addRole(Long memberId, String roleCode) {
        Long exists = memberRoleMapper.selectCount(Wrappers.<MemberRole>lambdaQuery()
                .eq(MemberRole::getMemberId, memberId)
                .eq(MemberRole::getRoleCode, roleCode));
        if (exists == null || exists == 0) {
            MemberRole memberRole = new MemberRole();
            memberRole.setMemberId(memberId);
            memberRole.setRoleCode(roleCode);
            memberRoleMapper.insert(memberRole);
        }
    }

    /**
     * 会员类型变化时只同步基础角色，保留干事、部长等管理角色。
     */
    private void syncBaseRole(Long memberId, String memberType) {
        String target = baseRoleOf(memberType);
        memberRoleMapper.delete(Wrappers.<MemberRole>lambdaQuery()
                .eq(MemberRole::getMemberId, memberId)
                .in(MemberRole::getRoleCode, BASE_ROLES)
                .ne(MemberRole::getRoleCode, target));
        addRole(memberId, target);
    }

    private String baseRoleOf(String memberType) {
        return switch (Enums.MemberType.valueOf(memberType)) {
            case OBSERVE_MEMBER -> Roles.OBSERVE_MEMBER;
            case MEMBER -> Roles.MEMBER;
            case LAB_MEMBER -> Roles.LAB_MEMBER;
        };
    }

    private String stringValue(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw BusinessException.badRequest(field + " 取值不合法：" + value);
        }
    }
}
