package com.scuec.icpc.module.mbti.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * MBTI 测评结果实体，对应 mbti_result 表。
 * 同一会员可多次测评，查询时取最新一次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbti_result")
public class MbtiResult extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long resultId;

    private Long memberId;

    /**
     * 四字母类型，如 INTJ。
     */
    private String type;

    /**
     * 各维度 A 侧得分占比（0-100），表字段为 score_ei / score_sn / score_tf / score_jp。
     */
    private Double scoreEi;
    private Double scoreSn;
    private Double scoreTf;
    private Double scoreJp;

    /**
     * 文本测评报告。
     */
    private String report;

    private LocalDateTime testTime;
}
