package com.zhs.dao;


import com.zhs.model.BasketMatchStatusLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 比赛状态变更记录表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMatchStatusLogDao extends BaseMapper<BasketMatchStatusLog> {

}
