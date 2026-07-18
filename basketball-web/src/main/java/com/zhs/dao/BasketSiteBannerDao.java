package com.zhs.dao;


import com.zhs.model.BasketSiteBanner;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 官网轮播图表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketSiteBannerDao extends BaseMapper<BasketSiteBanner> {

}
