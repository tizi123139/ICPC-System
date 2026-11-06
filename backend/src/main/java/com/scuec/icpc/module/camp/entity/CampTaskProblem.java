package com.scuec.icpc.module.camp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 集训任务-题目关联实体，对应 camp_task_problem 表。
 */
@Data
@TableName("camp_task_problem")
public class CampTaskProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long problemId;
    private Integer sortOrder;
}
