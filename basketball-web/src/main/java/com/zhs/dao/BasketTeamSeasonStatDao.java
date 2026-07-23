package com.zhs.dao;


import com.zhs.model.BasketTeamSeasonStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球队赛季汇总数据表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTeamSeasonStatDao extends BaseMapper<BasketTeamSeasonStat> {

}
