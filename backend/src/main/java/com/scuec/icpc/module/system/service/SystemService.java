package com.scuec.icpc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.common.security.SecurityUtils;
import com.scuec.icpc.module.file.entity.FileRecord;
import com.scuec.icpc.module.file.mapper.FileRecordMapper;
import com.scuec.icpc.module.log.entity.OperationLog;
import com.scuec.icpc.module.log.mapper.OperationLogMapper;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.entity.MemberRole;
import com.scuec.icpc.module.member.entity.Role;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.member.mapper.MemberRoleMapper;
import com.scuec.icpc.module.member.mapper.RoleMapper;
import com.scuec.icpc.module.system.dto.SystemDtos;
import com.scuec.icpc.module.system.entity.SystemConfig;
import com.scuec.icpc.module.system.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统管理业务：操作日志、角色字典、会员角色、系统配置与文件记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    /**
     * 对外展示的角色字典（与接口手册 0.5 的 7 类角色一致）。
     */
    private static final List<String> DOCUMENTED_ROLES = List.of(
            Roles.OBSERVE_MEMBER, Roles.MEMBER, Roles.TEAM_LEADER, Roles.OFFICER,
            Roles.ASSISTANT_COACH, Roles.PRESIDENT, Roles.ADVISOR);

    private final OperationLogMapper operationLogMapper;
    private final RoleMapper roleMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final MemberMapper memberMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final FileRecordMapper fileRecordMapper;

    // ==================== 操作日志 ====================

    public List<SystemDtos.LogVO> logs() {
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(1, 200),
                Wrappers.<OperationLog>lambdaQuery()
                        .orderByDesc(OperationLog::getCreateTime)
                        .orderByDesc(OperationLog::getLogId));
        return page.getRecords().stream().map(this::toLogVO).toList();
    }

    public SystemDtos.LogVO logDetail(Long logId) {
        OperationLog logEntity = operationLogMapper.selectById(logId);
        if (logEntity == null) {
            throw BusinessException.notFound("操作日志不存在");
        }
        return toLogVO(logEntity);
    }

    // ==================== 角色 ====================

    public List<SystemDtos.RoleVO> roles() {
        Map<String, Role> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getRoleCode, Function.identity(), (a, b) -> a));
        return DOCUMENTED_ROLES.stream()
                .map(code -> {
                    Role role = roleMap.get(code);
                    return SystemDtos.RoleVO.builder()
                            .roleCode(code)
                            .roleName(role == null ? Roles.displayName(code) : role.getRoleName())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void updateMemberRoles(Long memberId, SystemDtos.RolesRequest request) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BusinessException.notFound("会员不存在");
        }
        Set<String> allowed = new HashSet<>(Roles.ALL);
        List<String> roles = request.getRoles().stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (roles.isEmpty()) {
            throw BusinessException.badRequest("至少需要为会员保留一个角色");
        }
        for (String role : roles) {
            if (!allowed.contains(role)) {
                throw BusinessException.badRequest("角色编码不合法：" + role);
            }
        }
        memberRoleMapper.delete(Wrappers.<MemberRole>lambdaQuery()
                .eq(MemberRole::getMemberId, memberId));
        for (String role : roles) {
            MemberRole memberRole = new MemberRole();
            memberRole.setMemberId(memberId);
            memberRole.setRoleCode(role);
            memberRoleMapper.insert(memberRole);
        }
        log.info("管理员 {} 调整会员 {} 角色为 {}", SecurityUtils.currentMemberId(), memberId, roles);
    }

    // ==================== 系统配置 ====================

    public List<SystemDtos.ConfigVO> configs() {
        return systemConfigMapper.selectList(Wrappers.<SystemConfig>lambdaQuery()
                        .orderByAsc(SystemConfig::getConfigKey))
                .stream()
                .map(item -> SystemDtos.ConfigVO.builder()
                        .configKey(item.getConfigKey())
                        .configValue(item.getConfigValue())
                        .description(item.getDescription())
                        .updateTime(item.getUpdateTime())
                        .build())
                .toList();
    }

    @Transactional
    public void updateConfigs(SystemDtos.ConfigsRequest request) {
        for (SystemDtos.ConfigItem item : request.getConfigs()) {
            SystemConfig config = systemConfigMapper.selectById(item.getConfigKey());
            if (config == null) {
                config = new SystemConfig();
                config.setConfigKey(item.getConfigKey().trim());
                config.setConfigValue(item.getConfigValue());
                config.setUpdateTime(LocalDateTime.now());
                systemConfigMapper.insert(config);
            } else {
                config.setConfigValue(item.getConfigValue());
                config.setUpdateTime(LocalDateTime.now());
                systemConfigMapper.updateById(config);
            }
        }
    }

    // ==================== 文件记录 ====================

    public List<SystemDtos.FileVO> files() {
        Page<FileRecord> page = fileRecordMapper.selectPage(new Page<>(1, 200),
                Wrappers.<FileRecord>lambdaQuery()
                        .orderByDesc(FileRecord::getCreateTime)
                        .orderByDesc(FileRecord::getFileId));
        List<FileRecord> records = page.getRecords();
        Map<Long, Member> memberMap = loadMemberMap(records.stream()
                .map(FileRecord::getUploaderId).toList());
        return records.stream().map(record -> {
            Member uploader = memberMap.get(record.getUploaderId());
            return SystemDtos.FileVO.builder()
                    .fileId(record.getFileId())
                    .filename(record.getFilename())
                    .url(record.getUrl())
                    .size(record.getSize())
                    .uploaderName(uploader == null ? null : uploader.getName())
                    .createTime(record.getCreateTime())
                    .build();
        }).toList();
    }

    @Transactional
    public void deleteFile(Long fileId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            throw BusinessException.notFound("文件记录不存在");
        }
        fileRecordMapper.deleteById(fileId);
    }

    // ==================== 内部工具 ====================

    private SystemDtos.LogVO toLogVO(OperationLog logEntity) {
        String detail = (logEntity.getMethod() == null ? "" : logEntity.getMethod() + " ")
                + (logEntity.getPath() == null ? "" : logEntity.getPath());
        return SystemDtos.LogVO.builder()
                .logId(logEntity.getLogId())
                .userId(logEntity.getMemberId())
                .userName(logEntity.getUsername())
                .action(logEntity.getAction())
                .detail(detail.trim())
                .ip(logEntity.getIp())
                .createTime(logEntity.getCreateTime())
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
