package com.scuec.icpc.module.contest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 赛事-题目关联实体，对应 contest_problem 表。
 */
@Data
@TableName("contest_problem")
public class ContestProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contestId;
    private Long problemId;
    private Integer sortOrder;
}
