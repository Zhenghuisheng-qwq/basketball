package com.zhs.dao;


import com.zhs.model.BasketAccountSecurityLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 账号安全日志表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketAccountSecurityLogDao extends BaseMapper<BasketAccountSecurityLog> {

}
