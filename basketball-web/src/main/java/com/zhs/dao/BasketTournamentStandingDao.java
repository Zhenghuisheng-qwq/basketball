package com.zhs.dao;


import com.zhs.model.BasketTournamentStanding;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 赛事积分榜表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketTournamentStandingDao extends BaseMapper<BasketTournamentStanding> {

}
