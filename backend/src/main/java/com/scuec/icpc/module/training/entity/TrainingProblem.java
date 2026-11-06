package com.scuec.icpc.module.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 训练-题目关联实体，对应 training_problem 表。
 */
@Data
@TableName("training_problem")
public class TrainingProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long trainingId;
    private Long problemId;
    private Integer sortOrder;
}
