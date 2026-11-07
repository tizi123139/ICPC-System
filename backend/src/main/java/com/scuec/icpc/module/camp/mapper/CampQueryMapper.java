package com.scuec.icpc.module.camp.mapper;

import com.scuec.icpc.module.camp.dto.CampRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 集训模块的跨表查询。题目模块尚未落地，这里只读取 problem 表的只读字段。
 */
@Mapper
public interface CampQueryMapper {

    @Select("""
            <script>
            SELECT ctp.target_id    AS taskId,
                   ctp.problem_id   AS problemId,
                   p.title          AS title,
                   ctp.sort_order   AS sortOrder
            FROM problem_link ctp
            JOIN problem p ON p.problem_id = ctp.problem_id AND p.deleted = 0
            WHERE ctp.target_type = 'CAMP_TASK' AND ctp.target_id IN
            <foreach collection="taskIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY ctp.sort_order, ctp.id
            </script>
            """)
    List<CampRows.TaskProblemRow> selectTaskProblems(@Param("taskIds") Collection<Long> taskIds);

    @Select("""
            <script>
            SELECT COUNT(*) FROM problem
            WHERE deleted = 0 AND problem_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    long countExistingProblems(@Param("ids") Collection<Long> ids);
}
