package com.scuec.icpc.module.camp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 集训每日任务实体，对应 camp_task 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("camp_task")
public class CampTask extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    private Long campId;
    private String title;
    private String content;
    private LocalDate taskDate;
    private Long creatorId;
}
