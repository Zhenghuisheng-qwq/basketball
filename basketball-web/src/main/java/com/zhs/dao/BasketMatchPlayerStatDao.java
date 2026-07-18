package com.zhs.dao;


import com.zhs.model.BasketMatchPlayerStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 比赛球员数据表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketMatchPlayerStatDao extends BaseMapper<BasketMatchPlayerStat> {

}
