package com.zhs.dao;


import com.zhs.model.BasketEmailCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 邮箱验证码表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketEmailCodeDao extends BaseMapper<BasketEmailCode> {

}
