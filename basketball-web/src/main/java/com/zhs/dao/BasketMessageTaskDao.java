package com.zhs.dao;


import com.zhs.model.BasketMessageTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 消息任务表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMessageTaskDao extends BaseMapper<BasketMessageTask> {

}
