package com.scuec.icpc.module.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员-角色关联实体，对应 member_role 表。
 */
@Data
@TableName("member_role")
public class MemberRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;
    private String roleCode;
}
