package com.zhs.dao;


import com.zhs.model.BasketTeamCaptainHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球队队长变更记录表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketTeamCaptainHistoryDao extends BaseMapper<BasketTeamCaptainHistory> {

}
