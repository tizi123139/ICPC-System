package com.scuec.icpc.module.application.entity;

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
 * 通用申请实体，对应 application 表。
 * type 区分入会、退会、集训报名、赛事报名，applyData 保存各类型各自的表单 JSON。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "application", autoResultMap = true)
public class Application extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long applyId;

    /**
     * JOIN_MEMBER / RESIGN / CAMP_REGISTER / CONTEST_REGISTER。
     */
    private String type;

    /**
     * 集训或赛事主键；入会/退会申请为空。
     */
    private Long targetId;

    /**
     * 申请人；入会申请审核通过前为空。
     */
    private Long applicantId;

    /**
     * 赛事组队报名时的队伍 ID，其他申请为空。
     */
    private Long teamId;

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
