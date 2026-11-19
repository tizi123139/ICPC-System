package com.scuec.icpc.module.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scuec.icpc.module.problem.entity.Solution;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SolutionMapper extends BaseMapper<Solution> {
}
