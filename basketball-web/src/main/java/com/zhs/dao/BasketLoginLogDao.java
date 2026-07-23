package com.zhs.dao;


import com.zhs.model.BasketLoginLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 会员登录日志表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketLoginLogDao extends BaseMapper<BasketLoginLog> {

}
