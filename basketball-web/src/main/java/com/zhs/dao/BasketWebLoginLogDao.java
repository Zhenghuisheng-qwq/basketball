package com.zhs.dao;


import com.zhs.model.BasketWebLoginLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 网页登录日志表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketWebLoginLogDao extends BaseMapper<BasketWebLoginLog> {

}
