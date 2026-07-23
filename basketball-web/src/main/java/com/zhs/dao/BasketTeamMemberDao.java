package com.zhs.dao;


import com.zhs.model.BasketTeamMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球队成员表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTeamMemberDao extends BaseMapper<BasketTeamMember> {

}
