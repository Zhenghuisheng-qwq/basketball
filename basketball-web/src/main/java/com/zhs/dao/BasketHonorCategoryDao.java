package com.zhs.dao;


import com.zhs.model.BasketHonorCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 荣誉分类表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketHonorCategoryDao extends BaseMapper<BasketHonorCategory> {

}
