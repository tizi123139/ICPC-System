package com.scuec.icpc.module.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scuec.icpc.module.training.entity.Submission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
}
