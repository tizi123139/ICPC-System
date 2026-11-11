package com.scuec.icpc.module.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识点实体，对应 tag 表。通过 parent_id 自关联形成树形结构。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long tagId;

    private String name;

    /**
     * 知识点分类，如 DP / 图论 / 数论。
     */
    private String category;

    /**
     * 父知识点 ID，为空表示根节点。
     */
    private Long parentId;

    private Integer sortOrder;
}
