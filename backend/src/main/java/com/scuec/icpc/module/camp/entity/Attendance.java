package com.scuec.icpc.module.camp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 集训考勤实体，对应 attendance 表。
 * 同一集训、同一会员、同一天仅允许一条记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("attendance")
public class Attendance extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long recordId;

    private Long campId;
    private Long memberId;
    private LocalDate checkInDate;
    private LocalDateTime checkInTime;

    /**
     * PRESENT / ABSENT / LEAVE / MAKEUP。
     */
    private String status;

    private String remark;
}
