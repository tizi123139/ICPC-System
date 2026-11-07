package com.scuec.icpc.module.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 通用题目关联实体，对应 problem_link 表。
 * 训练、集训任务、赛事通过 targetType 区分归属。
 */
@Data
@TableName("problem_link")
public class ProblemLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * TRAINING / CAMP_TASK / CONTEST。
     */
    private String targetType;

    private Long targetId;
    private Long problemId;
    private Integer sortOrder;
}
