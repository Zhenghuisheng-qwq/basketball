package com.zhs.dao;


import com.zhs.model.BasketSiteMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 站内消息表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketSiteMessageDao extends BaseMapper<BasketSiteMessage> {

}
