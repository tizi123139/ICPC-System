package com.scuec.icpc.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置项，对应 system_config 表。
 */
@Data
@TableName("system_config")
public class SystemConfig {

    @TableId(type = IdType.INPUT)
    private String configKey;

    private String configValue;
    private String description;
    private LocalDateTime updateTime;
}
