package com.scuec.icpc.module.reward.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 奖励记录，对应 reward 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reward")
public class Reward extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long rewardId;

    private Long memberId;
    private String title;
    private String category;
    private String level;
    private BigDecimal amount;
    private String description;
    private LocalDate grantDate;
    private String issuerName;
    private Long creatorId;
}
