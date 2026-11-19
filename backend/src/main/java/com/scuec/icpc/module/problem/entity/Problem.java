package com.scuec.icpc.module.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目实体，对应 problem 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("problem")
public class Problem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long problemId;

    private String title;

    /**
     * 自建 OJ 的题目编号。
     */
    private String ojProblemId;

    /**
     * EASY / MEDIUM / HARD。
     */
    private String difficulty;

    /**
     * 时间限制（毫秒）。
     */
    private Integer timeLimit;

    /**
     * 内存限制（KB）。
     */
    private Integer memoryLimit;

    private String description;
    private String inputFormat;
    private String outputFormat;
    private String sampleInput;
    private String sampleOutput;
    private String hint;

    /**
     * PUBLIC / TRAINING_PRIVATE。
     */
    private String visibility;

    private Long creatorId;
}
