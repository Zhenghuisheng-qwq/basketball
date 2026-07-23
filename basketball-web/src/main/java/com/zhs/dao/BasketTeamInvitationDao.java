package com.zhs.dao;


import com.zhs.model.BasketTeamInvitation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球队邀请表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTeamInvitationDao extends BaseMapper<BasketTeamInvitation> {

}
