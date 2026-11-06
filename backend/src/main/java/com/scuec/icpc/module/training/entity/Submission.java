package com.scuec.icpc.module.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 代码提交实体，对应 submission 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("submission")
public class Submission extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long submitId;

    private Long trainingId;
    private Long contestId;
    private Long problemId;
    private Long submitterId;
    private String code;
    private String language;

    /**
     * PENDING / JUDGING / AC / WA / TLE / MLE / RE / CE。
     */
    private String result;

    private Integer timeUsed;
    private Integer memoryUsed;
    private String ojSubmitId;
    private LocalDateTime submitTime;
}
