package com.zhs.dao;


import com.zhs.model.BasketTeamJoinApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球队加入申请表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTeamJoinApplicationDao extends BaseMapper<BasketTeamJoinApplication> {

}
