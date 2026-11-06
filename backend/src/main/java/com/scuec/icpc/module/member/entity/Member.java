package com.scuec.icpc.module.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员实体，对应 member 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member")
public class Member extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long memberId;

    private String studentId;

    /**
     * BCrypt 加密后的密码，禁止直接序列化给前端。
     */
    @JsonIgnore
    private String password;

    private String name;
    private String major;
    private String grade;
    private String phone;
    private String email;
    private String avatar;

    /**
     * 会员类型：OBSERVE_MEMBER / MEMBER / LAB_MEMBER。
     */
    private String memberType;

    private String department;
    private LocalDate joinDate;

    /**
     * 会员状态：ACTIVE / INACTIVE / RESIGNED / REMOVED。
     */
    private String status;

    private LocalDateTime lastLoginTime;
}
