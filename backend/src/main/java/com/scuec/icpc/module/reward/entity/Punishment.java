package com.scuec.icpc.module.reward.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 处分记录，对应 punishment 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("punishment")
public class Punishment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long punishmentId;

    private Long memberId;
    private String type;
    private String reason;
    private String description;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String issuerName;
    private Long creatorId;
}
