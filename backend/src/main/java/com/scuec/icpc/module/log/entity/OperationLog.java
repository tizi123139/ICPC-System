package com.scuec.icpc.module.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体，对应 operation_log 表。
 * 该表只追加不修改，因此不继承 BaseEntity。
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long memberId;
    private String username;
    private String method;
    private String path;
    private String action;
    private String ip;
    private Integer status;
    private Long duration;
    private LocalDateTime createTime;
}
