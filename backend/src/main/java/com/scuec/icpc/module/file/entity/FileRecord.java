package com.scuec.icpc.module.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scuec.icpc.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件上传记录，对应 file_record 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
public class FileRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long fileId;

    /**
     * 原始文件名。
     */
    private String filename;

    /**
     * 落盘文件名（UUID + 扩展名）。
     */
    private String storedName;

    /**
     * 可访问的相对 URL，如 /uploads/2026/11/xxx.png。
     */
    private String url;

    private Long size;

    private String contentType;

    private Long uploaderId;
}
