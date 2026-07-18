package com.zhs.dao;


import com.zhs.model.BasketTeamLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球队操作记录表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketTeamLogDao extends BaseMapper<BasketTeamLog> {

}
