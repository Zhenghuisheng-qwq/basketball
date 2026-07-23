package com.zhs.dao;


import com.zhs.model.BasketMatchTeam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 比赛球队表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMatchTeamDao extends BaseMapper<BasketMatchTeam> {

}
