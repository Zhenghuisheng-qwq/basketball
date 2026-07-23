package com.zhs.dao;


import com.zhs.model.BasketTournamentRegistrationPlayer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 赛事报名球员表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTournamentRegistrationPlayerDao extends BaseMapper<BasketTournamentRegistrationPlayer> {

}
