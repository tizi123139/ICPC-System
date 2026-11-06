package com.scuec.icpc.module.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色字典实体，对应 sys_role 表。
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(type = IdType.INPUT)
    private String roleCode;

    private String roleName;
    private String description;
}
