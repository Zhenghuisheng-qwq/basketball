package com.zhs.dao;


import com.zhs.model.BasketMessageReceiver;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 消息接收记录表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketMessageReceiverDao extends BaseMapper<BasketMessageReceiver> {

}
