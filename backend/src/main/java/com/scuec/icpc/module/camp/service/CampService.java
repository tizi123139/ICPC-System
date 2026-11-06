package com.scuec.icpc.module.camp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.enums.Enums;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.camp.dto.CampDtos;
import com.scuec.icpc.module.camp.dto.CampRows;
import com.scuec.icpc.module.camp.entity.Attendance;
import com.scuec.icpc.module.camp.entity.Camp;
import com.scuec.icpc.module.camp.entity.CampRegistration;
import com.scuec.icpc.module.camp.entity.CampTask;
import com.scuec.icpc.module.camp.entity.CampTaskProblem;
import com.scuec.icpc.module.camp.mapper.AttendanceMapper;
import com.scuec.icpc.module.camp.mapper.CampMapper;
import com.scuec.icpc.module.camp.mapper.CampQueryMapper;
import com.scuec.icpc.module.camp.mapper.CampRegistrationMapper;
import com.scuec.icpc.module.camp.mapper.CampTaskMapper;
import com.scuec.icpc.module.camp.mapper.CampTaskProblemMapper;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集训管理业务：集训发布、报名审核、每日任务、签到与考勤。
 * 寒假与暑假集训共用同一套接口，通过 type 区分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampService {

    private final CampMapper campMapper;
    private final CampRegistrationMapper campRegistrationMapper;
    private final CampTaskMapper campTaskMapper;
    private final CampTaskProblemMapper campTaskProblemMapper;
    private final AttendanceMapper attendanceMapper;
    private final CampQueryMapper queryMapper;
    private final MemberMapper memberMapper;

    // ==================== 集训信息 ====================

    public List<CampDtos.CampVO> list(CampDtos.Query query) {
        String type = parseEnum(Enums.CampType.class, query.getType(), "type").name();
        LambdaQueryWrapper<Camp> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Camp::getType, type);
        if (query.getYear() != null) {
            if (query.getYear() < 2000 || query.getYear() > 2100) {
                throw BusinessException.badRequest("year 取值不合法：" + query.getYear());
            }
            LocalDateTime yearStart = LocalDate.of(query.getYear(), 1, 1).atStartOfDay();
            wrapper.ge(Camp::getStartTime, yearStart)
                    .lt(Camp::getStartTime, yearStart.plusYears(1));
        }
        wrapper.orderByDesc(Camp::getStartTime).orderByDesc(Camp::getCampId)
                .last("LIMIT 200");

        List<Camp> camps = campMapper.selectList(wrapper);
        if (camps.isEmpty()) {
            return List.of();
        }
        Map<Long, Member> creatorMap = loadMembers(camps.stream()
                .map(Camp::getCreatorId).toList());
        LocalDateTime now = LocalDateTime.now();
        return camps.stream()
                .map(camp -> toVO(camp, now, creatorMap.get(camp.getCreatorId())))
                .toList();
    }

    public CampDtos.CampDetailVO detail(Long campId) {
        Camp camp = getOrThrow(campId);
        List<CampTask> tasks = campTaskMapper.selectList(Wrappers.<CampTask>lambdaQuery()
                .eq(CampTask::getCampId, campId)
                .orderByAsc(CampTask::getTaskDate)
                .orderByAsc(CampTask::getTaskId));
        Map<Long, List<CampRows.TaskProblemRow>> problemMap = loadTaskProblems(
                tasks.stream().map(CampTask::getTaskId).toList());
        Member creator = camp.getCreatorId() == null
                ? null : memberMapper.selectById(camp.getCreatorId());

        List<CampDtos.TaskVO> taskVOs = tasks.stream()
                .map(task -> toTaskVO(task,
                        problemMap.getOrDefault(task.getTaskId(), List.of())))
                .toList();
        return CampDtos.CampDetailVO.builder()
                .basicInfo(toVO(camp, LocalDateTime.now(), creator))
                .tasks(taskVOs)
                .build();
    }

    @Transactional
    public CampDtos.CampVO create(CampDtos.CreateRequest request) {
        String type = parseEnum(Enums.CampType.class, request.getType(), "type").name();
        validateCampTime(request.getStartTime(), request.getEndTime(),
                request.getRegistrationDeadline());

        Camp camp = new Camp();
        camp.setType(type);
        camp.setName(request.getName().trim());
        camp.setStartTime(request.getStartTime());
        camp.setEndTime(request.getEndTime());
        camp.setLocation(request.getLocation());
        camp.setRegistrationDeadline(request.getRegistrationDeadline());
        camp.setDescription(request.getDescription());
        camp.setRequirements(request.getRequirements());
        camp.setStatus(Enums.CampStatus.ANNOUNCED.name());
        camp.setCreatorId(SecurityUtils.currentMemberId());
        campMapper.insert(camp);

        log.info("发布集训 {}（{}），类型 {}，时间 {} ~ {}",
                camp.getCampId(), camp.getName(), type,
                camp.getStartTime(), camp.getEndTime());
        return toVO(camp, LocalDateTime.now(), memberMapper.selectById(camp.getCreatorId()));
    }

    // ==================== 报名与审核 ====================

    @Transactional
    public void register(Long campId, CampDtos.RegistrationRequest request) {
        Camp camp = getOrThrow(campId);
        Long memberId = SecurityUtils.currentMemberId();
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.notFound("会员不存在");
        }
        if (!Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.conflict("当前会员状态不允许报名集训");
        }

        LocalDateTime now = LocalDateTime.now();
        if (camp.getRegistrationDeadline() != null
                && now.isAfter(camp.getRegistrationDeadline())) {
            throw BusinessException.conflict("集训报名已截止");
        }
        if (!now.isBefore(camp.getStartTime())) {
            throw BusinessException.conflict("集训已开始，无法报名");
        }

        Long duplicated = campRegistrationMapper.selectCount(
                Wrappers.<CampRegistration>lambdaQuery()
                        .eq(CampRegistration::getCampId, campId)
                        .eq(CampRegistration::getMemberId, memberId)
                        .in(CampRegistration::getStatus,
                                Enums.ApplyStatus.PENDING.name(),
                                Enums.ApplyStatus.APPROVED.name()));
        if (duplicated != null && duplicated > 0) {
            throw BusinessException.conflict("已提交过集训报名，请勿重复提交");
        }
        validateAvailableDates(camp, request.getAvailableDates());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reason", request.getReason());
        data.put("availableDates", request.getAvailableDates() == null
                ? List.of()
                : request.getAvailableDates().stream().map(LocalDate::toString).toList());

        CampRegistration registration = new CampRegistration();
        registration.setCampId(campId);
        registration.setMemberId(memberId);
        registration.setApplyData(data);
        registration.setStatus(Enums.ApplyStatus.PENDING.name());
        registration.setApplyTime(now);
        campRegistrationMapper.insert(registration);

        log.info("会员 {} 提交集训 {} 报名，报名记录 {}",
                memberId, campId, registration.getRegistrationId());
    }

    public List<CampDtos.RegistrationVO> registrations(Long campId,
                                                      CampDtos.RegistrationQuery query) {
        getOrThrow(campId);
        LambdaQueryWrapper<CampRegistration> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(CampRegistration::getCampId, campId);
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CampRegistration::getStatus,
                    parseEnum(Enums.ApplyStatus.class, query.getStatus(), "status").name());
        }
        wrapper.orderByDesc(CampRegistration::getApplyTime)
                .orderByDesc(CampRegistration::getRegistrationId)
                .last("LIMIT 500");

        List<CampRegistration> registrations = campRegistrationMapper.selectList(wrapper);
        if (registrations.isEmpty()) {
            return List.of();
        }
        List<Long> memberIds = new ArrayList<>();
        registrations.forEach(registration -> {
            memberIds.add(registration.getMemberId());
            if (registration.getAuditorId() != null) {
                memberIds.add(registration.getAuditorId());
            }
        });
        Map<Long, Member> memberMap = loadMembers(memberIds);
        return registrations.stream()
                .map(registration -> toRegistrationVO(registration, memberMap))
                .toList();
    }

    @Transactional
    public void audit(Long campId, Long applyId, CampDtos.AuditRequest request) {
        getOrThrow(campId);
        CampRegistration registration = campRegistrationMapper.selectById(applyId);
        if (registration == null || !campId.equals(registration.getCampId())) {
            throw BusinessException.notFound("集训报名记录不存在");
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
        campRegistrationMapper.updateById(registration);

        log.info("集训 {} 报名 {} 审批结果：{}",
                campId, applyId, registration.getStatus());
    }

    // ==================== 每日任务 ====================

    @Transactional
    public CampDtos.TaskVO createTask(Long campId, CampDtos.TaskCreateRequest request) {
        Camp camp = getOrThrow(campId);
        validateTaskDate(camp, request.getDate());

        List<Long> problemIds = distinctProblems(request.getProblemIds());
        validateProblems(problemIds);

        CampTask task = new CampTask();
        task.setCampId(campId);
        task.setTitle(request.getTitle().trim());
        task.setContent(request.getContent());
        task.setTaskDate(request.getDate());
        task.setCreatorId(SecurityUtils.currentMemberId());
        campTaskMapper.insert(task);
        saveTaskProblems(task.getTaskId(), problemIds);

        return toTaskVO(task, loadTaskProblems(List.of(task.getTaskId()))
                .getOrDefault(task.getTaskId(), List.of()));
    }

    // ==================== 签到与考勤 ====================

    @Transactional
    public CampDtos.AttendanceVO checkin(Long campId, CampDtos.CheckInRequest request) {
        Camp camp = getOrThrow(campId);
        Long memberId = SecurityUtils.currentMemberId();
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.notFound("会员不存在");
        }
        if (!Enums.MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw BusinessException.conflict("当前会员状态不允许签到");
        }
        validateCheckInDate(camp, request.getDate());

        Long approved = campRegistrationMapper.selectCount(
                Wrappers.<CampRegistration>lambdaQuery()
                        .eq(CampRegistration::getCampId, campId)
                        .eq(CampRegistration::getMemberId, memberId)
                        .eq(CampRegistration::getStatus, Enums.ApplyStatus.APPROVED.name()));
        if (approved == null || approved == 0) {
            throw BusinessException.forbidden("仅报名审核通过的学员可签到");
        }

        Long exists = attendanceMapper.selectCount(Wrappers.<Attendance>lambdaQuery()
                .eq(Attendance::getCampId, campId)
                .eq(Attendance::getMemberId, memberId)
                .eq(Attendance::getCheckInDate, request.getDate()));
        if (exists != null && exists > 0) {
            throw BusinessException.conflict("当天已签到，请勿重复打卡");
        }

        Attendance attendance = new Attendance();
        attendance.setCampId(campId);
        attendance.setMemberId(memberId);
        attendance.setCheckInDate(request.getDate());
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setStatus(Enums.AttendanceStatus.PRESENT.name());
        attendance.setRemark(request.getRemark());
        attendanceMapper.insert(attendance);

        log.info("会员 {} 完成集训 {} 的 {} 签到",
                memberId, campId, request.getDate());
        return toAttendanceVO(attendance, member);
    }

    public List<CampDtos.AttendanceVO> attendance(Long campId,
                                                  CampDtos.AttendanceQuery query) {
        getOrThrow(campId);
        boolean manager = SecurityUtils.isAtLeast(Roles.ASSISTANT_COACH);
        Long targetUserId = query.getUserId();
        if (targetUserId != null) {
            if (!targetUserId.equals(SecurityUtils.currentMemberId()) && !manager) {
                throw BusinessException.forbidden("只能查看自己的考勤记录");
            }
        } else if (!manager) {
            targetUserId = SecurityUtils.currentMemberId();
        }

        LambdaQueryWrapper<Attendance> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Attendance::getCampId, campId);
        if (targetUserId != null) {
            wrapper.eq(Attendance::getMemberId, targetUserId);
        }
        if (query.getDate() != null) {
            wrapper.eq(Attendance::getCheckInDate, query.getDate());
        }
        wrapper.orderByDesc(Attendance::getCheckInDate)
                .orderByDesc(Attendance::getRecordId)
                .last("LIMIT 500");

        List<Attendance> records = attendanceMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, Member> memberMap = loadMembers(records.stream()
                .map(Attendance::getMemberId).toList());
        return records.stream()
                .map(record -> toAttendanceVO(record, memberMap.get(record.getMemberId())))
                .toList();
    }

    @Transactional
    public CampDtos.AttendanceVO updateAttendance(Long campId, Long recordId,
                                                  CampDtos.AttendanceUpdateRequest request) {
        getOrThrow(campId);
        Attendance attendance = attendanceMapper.selectById(recordId);
        if (attendance == null || !campId.equals(attendance.getCampId())) {
            throw BusinessException.notFound("考勤记录不存在");
        }
        Enums.AttendanceStatus status = parseEnum(Enums.AttendanceStatus.class,
                request.getStatus(), "status");

        attendance.setStatus(status.name());
        attendance.setRemark(request.getRemark());
        if (status == Enums.AttendanceStatus.MAKEUP && attendance.getCheckInTime() == null) {
            attendance.setCheckInTime(LocalDateTime.now());
        }
        attendanceMapper.updateById(attendance);

        log.info("考勤记录 {} 状态调整为 {}", recordId, status);
        return toAttendanceVO(attendance,
                memberMapper.selectById(attendance.getMemberId()));
    }

    // ==================== 内部工具 ====================

    private Camp getOrThrow(Long campId) {
        Camp camp = campMapper.selectById(campId);
        if (camp == null) {
            throw BusinessException.notFound("集训不存在");
        }
        return camp;
    }

    private String effectiveStatus(Camp camp, LocalDateTime now) {
        if (!now.isBefore(camp.getEndTime())) {
            return Enums.CampStatus.ENDED.name();
        }
        if (!now.isBefore(camp.getStartTime())) {
            return Enums.CampStatus.ONGOING.name();
        }
        if (camp.getRegistrationDeadline() != null
                && !now.isAfter(camp.getRegistrationDeadline())) {
            return Enums.CampStatus.REGISTERING.name();
        }
        return Enums.CampStatus.ANNOUNCED.name();
    }

    private CampDtos.CampVO toVO(Camp camp, LocalDateTime now, Member creator) {
        CampDtos.UserBrief creatorBrief = creator == null
                ? null
                : CampDtos.UserBrief.builder()
                        .memberId(creator.getMemberId())
                        .studentId(creator.getStudentId())
                        .name(creator.getName())
                        .build();
        return CampDtos.CampVO.builder()
                .campId(camp.getCampId())
                .name(camp.getName())
                .type(camp.getType())
                .startTime(camp.getStartTime())
                .endTime(camp.getEndTime())
                .location(camp.getLocation())
                .registrationDeadline(camp.getRegistrationDeadline())
                .description(camp.getDescription())
                .requirements(camp.getRequirements())
                .status(effectiveStatus(camp, now))
                .creator(creatorBrief)
                .createTime(camp.getCreateTime())
                .updateTime(camp.getUpdateTime())
                .build();
    }

    private CampDtos.TaskVO toTaskVO(CampTask task,
                                     List<CampRows.TaskProblemRow> problemRows) {
        List<CampDtos.TaskProblem> problems = problemRows.stream()
                .map(row -> CampDtos.TaskProblem.builder()
                        .problemId(row.getProblemId())
                        .title(row.getTitle())
                        .build())
                .toList();
        return CampDtos.TaskVO.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .date(task.getTaskDate())
                .content(task.getContent())
                .problemIds(problemRows.stream()
                        .map(CampRows.TaskProblemRow::getProblemId).toList())
                .problems(problems)
                .attachments(List.of())
                .build();
    }

    private CampDtos.RegistrationVO toRegistrationVO(CampRegistration registration,
                                                     Map<Long, Member> memberMap) {
        Member applicant = memberMap.get(registration.getMemberId());
        Member auditor = registration.getAuditorId() == null
                ? null : memberMap.get(registration.getAuditorId());
        return CampDtos.RegistrationVO.builder()
                .applyId(registration.getRegistrationId())
                .campId(registration.getCampId())
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

    private CampDtos.AttendanceVO toAttendanceVO(Attendance attendance, Member member) {
        return CampDtos.AttendanceVO.builder()
                .recordId(attendance.getRecordId())
                .campId(attendance.getCampId())
                .userId(attendance.getMemberId())
                .userName(member == null ? null : member.getName())
                .studentId(member == null ? null : member.getStudentId())
                .checkInDate(attendance.getCheckInDate())
                .checkInTime(attendance.getCheckInTime())
                .status(attendance.getStatus())
                .remark(attendance.getRemark())
                .build();
    }

    private Map<Long, List<CampRows.TaskProblemRow>> loadTaskProblems(
            Collection<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return queryMapper.selectTaskProblems(taskIds).stream()
                .collect(Collectors.groupingBy(CampRows.TaskProblemRow::getTaskId,
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

    private void saveTaskProblems(Long taskId, List<Long> problemIds) {
        int sort = 0;
        for (Long problemId : problemIds) {
            CampTaskProblem link = new CampTaskProblem();
            link.setTaskId(taskId);
            link.setProblemId(problemId);
            link.setSortOrder(sort++);
            campTaskProblemMapper.insert(link);
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
            return;
        }
        long existing = queryMapper.countExistingProblems(problemIds);
        if (existing != problemIds.size()) {
            throw BusinessException.badRequest("部分题目不存在或已被删除");
        }
    }

    private void validateCampTime(LocalDateTime startTime, LocalDateTime endTime,
                                  LocalDateTime registrationDeadline) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw BusinessException.badRequest("集训开始时间不能早于当前时间");
        }
        if (!startTime.isBefore(endTime)) {
            throw BusinessException.badRequest("集训开始时间必须早于结束时间");
        }
        if (registrationDeadline.isAfter(endTime)) {
            throw BusinessException.badRequest("报名截止时间不能晚于集训结束时间");
        }
    }

    private void validateTaskDate(Camp camp, LocalDate date) {
        LocalDate startDate = camp.getStartTime().toLocalDate();
        LocalDate endDate = camp.getEndTime().toLocalDate();
        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            throw BusinessException.badRequest("任务日期必须落在集训时间范围内");
        }
    }

    private void validateCheckInDate(Camp camp, LocalDate date) {
        LocalDate startDate = camp.getStartTime().toLocalDate();
        LocalDate endDate = camp.getEndTime().toLocalDate();
        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            throw BusinessException.badRequest("签到日期不在集训时间范围内");
        }
        if (date.isAfter(LocalDate.now())) {
            throw BusinessException.badRequest("不能提前签到");
        }
    }

    private void validateAvailableDates(Camp camp, List<LocalDate> availableDates) {
        if (availableDates == null || availableDates.isEmpty()) {
            return;
        }
        LocalDate startDate = camp.getStartTime().toLocalDate();
        LocalDate endDate = camp.getEndTime().toLocalDate();
        for (LocalDate date : availableDates) {
            if (date == null || date.isBefore(startDate) || date.isAfter(endDate)) {
                throw BusinessException.badRequest("可参加日期必须落在集训时间范围内");
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw BusinessException.badRequest(field + " 取值不合法：" + value);
        }
    }
}
