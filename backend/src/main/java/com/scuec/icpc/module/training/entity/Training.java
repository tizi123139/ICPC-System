package com.scuec.icpc.module.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 训练实体，对应 training 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("training")
public class Training extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long trainingId;

    private String name;

    /**
     * SOLO / TEAM。
     */
    private String type;

    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean leaderboardVisible;

    /**
     * 结束前 N 分钟封榜，0 表示不封榜。
     */
    private Integer freezeTime;

    /**
     * DRAFT / PUBLISHED / ONGOING / ENDED。
     */
    private String status;

    private Long creatorId;
}
