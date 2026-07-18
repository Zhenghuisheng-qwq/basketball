package com.zhs.dao;


import com.zhs.model.BasketTournamentTeam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 赛事球队快照表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketTournamentTeamDao extends BaseMapper<BasketTournamentTeam> {

}
