package com.scuec.icpc.module.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scuec.icpc.module.team.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {
}
