package com.zhs.dao;


import com.zhs.model.BasketHonor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 名人堂荣誉表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketHonorDao extends BaseMapper<BasketHonor> {

}
