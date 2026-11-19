package com.scuec.icpc.module.problem.mapper;

import com.scuec.icpc.module.problem.dto.ProblemRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 题库模块的跨表查询。
 */
@Mapper
public interface ProblemQueryMapper {

    /**
     * 批量查询题目关联的知识点，用于列表页一次性组装 tags，避免 N+1。
     */
    @Select("""
            <script>
            SELECT pt.problem_id AS problemId,
                   t.tag_id      AS tagId,
                   t.name        AS name,
                   t.category    AS category
            FROM problem_tag pt
            JOIN tag t ON t.tag_id = pt.tag_id AND t.deleted = 0
            WHERE pt.problem_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY t.sort_order, t.tag_id
            </script>
            """)
    List<ProblemRows.ProblemTagRow> selectTagsByProblemIds(@Param("ids") Collection<Long> ids);
}
