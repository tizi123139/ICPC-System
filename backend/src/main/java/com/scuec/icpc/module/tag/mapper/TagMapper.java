package com.scuec.icpc.module.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scuec.icpc.module.tag.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 统计该知识点下挂载的题目数量。problem_tag 为纯关联表，无逻辑删除字段。
     */
    @Select("""
            SELECT COUNT(*)
            FROM problem_tag
            WHERE tag_id = #{tagId}
            """)
    long countProblemLinks(@Param("tagId") Long tagId);
}
