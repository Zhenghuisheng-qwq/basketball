package com.zhs.dao;


import com.zhs.model.BasketRegistrationPlayer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 报名球员表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketRegistrationPlayerDao extends BaseMapper<BasketRegistrationPlayer> {

}
