package com.scuec.icpc.module.camp.entity;

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
 * 集训报名实体，对应 camp_registration 表。
 * applyData 为 JSON 字段，保存报名理由、可参加日期等信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "camp_registration", autoResultMap = true)
public class CampRegistration extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long registrationId;

    private Long campId;
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
