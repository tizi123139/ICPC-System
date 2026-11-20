package com.scuec.icpc.module.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考核成绩，对应 assessment_result 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_result")
public class AssessmentResult extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long resultId;

    private Long assessmentId;
    private Long memberId;
    private BigDecimal score;
    private String remark;
    private String auditStatus;
    private String auditRemark;
    private Long auditorId;
    private LocalDateTime auditTime;
}
