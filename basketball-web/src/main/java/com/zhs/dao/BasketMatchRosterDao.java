package com.zhs.dao;


import com.zhs.model.BasketMatchRoster;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 比赛出场名单表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMatchRosterDao extends BaseMapper<BasketMatchRoster> {

}
