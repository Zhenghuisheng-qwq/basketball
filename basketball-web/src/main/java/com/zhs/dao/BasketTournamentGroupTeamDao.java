package com.zhs.dao;


import com.zhs.model.BasketTournamentGroupTeam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 赛事分组球队表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTournamentGroupTeamDao extends BaseMapper<BasketTournamentGroupTeam> {

}
