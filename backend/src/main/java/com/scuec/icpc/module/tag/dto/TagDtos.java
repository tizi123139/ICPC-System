package com.scuec.icpc.module.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识点模块请求与响应对象。
 */
public final class TagDtos {

    private TagDtos() {
    }

    /**
     * 知识点树节点，children 递归嵌套。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagVO {
        private Long tagId;
        private String name;
        private String category;
        private Long parentId;
        private Integer sortOrder;
        private List<TagVO> children;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "知识点名称不能为空")
        @Size(max = 100, message = "知识点名称长度不能超过 100")
        private String name;

        @NotBlank(message = "知识点分类不能为空")
        @Size(max = 100, message = "知识点分类长度不能超过 100")
        private String category;

        /**
         * 父知识点 ID，null 或 0 表示根节点。
         */
        private Long parentId;

        private Integer sortOrder;
    }

    @Data
    public static class UpdateRequest {

        @Size(max = 100, message = "知识点名称长度不能超过 100")
        private String name;

        @Size(max = 100, message = "知识点分类长度不能超过 100")
        private String category;

        /**
         * 父知识点 ID：null 表示保持不变，0 表示移动到根节点。
         */
        private Long parentId;

        private Integer sortOrder;
    }
}
