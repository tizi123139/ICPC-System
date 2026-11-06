package com.scuec.icpc.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scuec.icpc.module.member.entity.MemberRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MemberRoleMapper extends BaseMapper<MemberRole> {

    @Select("SELECT role_code FROM member_role WHERE member_id = #{memberId}")
    List<String> selectRoleCodes(@Param("memberId") Long memberId);
}
