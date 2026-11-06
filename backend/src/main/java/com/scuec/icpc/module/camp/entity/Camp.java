package com.scuec.icpc.module.camp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 集训实体，对应 camp 表。寒假与暑假集训共用一张表，通过 type 区分。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("camp")
public class Camp extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long campId;

    /**
     * WINTER / SUMMER。
     */
    private String type;

    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private LocalDateTime registrationDeadline;
    private String description;
    private String requirements;

    /**
     * ANNOUNCED / REGISTERING / ONGOING / ENDED。
     * 实际状态按时间推导，入库值仅作为初始声明状态。
     */
    private String status;

    private Long creatorId;
}
