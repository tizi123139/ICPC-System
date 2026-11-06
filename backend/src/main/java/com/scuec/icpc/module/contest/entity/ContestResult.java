package com.scuec.icpc.module.contest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 赛事成绩与获奖实体，对应 contest_result 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contest_result")
public class ContestResult extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long resultId;

    private Long contestId;
    private Long teamId;
    private Long memberId;
    private Integer rankNo;
    private Integer solvedCount;
    private Integer penalty;
    private BigDecimal score;
    private String award;
    private String remark;
}
