package com.scuec.icpc.module.member.entity;

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
 * 入会 / 退会申请实体，对应 member_apply 表。
 * applyData 为 JSON 字段，不同申请类型的结构不同。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "member_apply", autoResultMap = true)
public class MemberApply extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long applyId;

    /**
     * JOIN_MEMBER / RESIGN / CAMP_REGISTER / CONTEST_REGISTER。
     */
    private String type;

    private Long applicantId;

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
