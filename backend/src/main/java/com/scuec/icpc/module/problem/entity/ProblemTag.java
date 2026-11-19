package com.scuec.icpc.module.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目-知识点关联实体，对应 problem_tag 表。
 * 该表为纯关联表，没有审计字段与逻辑删除字段，因此不继承 BaseEntity。
 */
@Data
@TableName("problem_tag")
public class ProblemTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long problemId;

    private Long tagId;
}
