package com.scuec.icpc.module.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题解实体，对应 solution 表。每道题最多一条题解。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("solution")
public class Solution extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long solutionId;

    private Long problemId;

    /**
     * Markdown 题解内容。
     */
    private String content;

    private String referenceCode;
}
