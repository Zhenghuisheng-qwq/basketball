package com.zhs.dao;


import com.zhs.model.BasketWebUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 网页账号表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketWebUserDao extends BaseMapper<BasketWebUser> {

}
