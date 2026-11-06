package com.scuec.icpc.module.mbti.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MBTI 测评题目实体，对应 mbti_question 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbti_question")
public class MbtiQuestion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long questionId;

    private Integer questionNo;

    /**
     * E_I / S_N / T_F / J_P。
     */
    private String dimension;

    private String content;
    private String optionA;
    private String optionB;
    private Integer scoreA;
    private Integer scoreB;
}
