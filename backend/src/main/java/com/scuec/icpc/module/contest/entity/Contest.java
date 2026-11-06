package com.scuec.icpc.module.contest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 赛事实体，对应 contest 表。校赛 / XCPC / 非 XCPC 共用，通过 category 区分。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contest")
public class Contest extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long contestId;

    private String name;

    /**
     * SCHOLASTIC / XCPC / NON_XCPC。
     */
    private String category;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registrationDeadline;
    private String description;
    private String requirements;
    private Integer maxTeamSize;
    private Integer freezeMinutes;

    /**
     * 入库初始状态，实际状态按时间推导。
     */
    private String status;

    private String ojContestId;
    private Long creatorId;
}
