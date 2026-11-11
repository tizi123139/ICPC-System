package com.scuec.icpc.module.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 队伍成员关联，对应 team_member 表。
 * 该表没有审计字段与逻辑删除，移除成员为物理删除。
 */
@Data
@TableName("team_member")
public class TeamMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long memberId;

    private LocalDateTime joinTime;
}
