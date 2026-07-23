package com.zhs.dao;


import com.zhs.model.BasketStageStanding;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 赛事阶段积分表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketStageStandingDao extends BaseMapper<BasketStageStanding> {

}
