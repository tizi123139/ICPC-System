package com.scuec.icpc.module.contest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 赛事报名实体，对应 contest_registration 表。
 * 个人赛 teamId 为空；组队赛以队伍为单位报名。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "contest_registration", autoResultMap = true)
public class ContestRegistration extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long registrationId;

    private Long contestId;
    private Long teamId;
    private Long memberId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> applyData;

    /**
     * PENDING / APPROVED / REJECTED。
     */
    private String status;

    private String auditRemark;
    private Long auditorId;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
}
