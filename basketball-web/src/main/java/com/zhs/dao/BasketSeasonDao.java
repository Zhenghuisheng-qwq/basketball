package com.zhs.dao;


import com.zhs.model.BasketSeason;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 赛季信息表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketSeasonDao extends BaseMapper<BasketSeason> {

}
