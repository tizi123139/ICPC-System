package com.scuec.icpc.module.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 考核，对应 assessment 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment")
public class Assessment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long assessmentId;

    private String name;
    private String type;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer passScore;
    private String status;
    private Long creatorId;
}
