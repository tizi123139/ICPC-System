package com.scuec.icpc.module.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 队伍实体，对应 team 表。
 * 状态流转：PENDING_CONFIRM（创建后待教练确认）→ CONFIRMED（确认后成员锁定）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("team")
public class Team extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long teamId;

    private String name;

    /**
     * 队长会员 ID。
     */
    private Long leaderId;

    /**
     * 目标赛事名称。当前表结构未关联 contest_id。
     */
    private String contestTarget;

    private String description;

    private String status;
}
