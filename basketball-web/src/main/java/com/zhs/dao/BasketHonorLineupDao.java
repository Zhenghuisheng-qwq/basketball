package com.zhs.dao;


import com.zhs.model.BasketHonorLineup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 冠军荣誉阵容表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketHonorLineupDao extends BaseMapper<BasketHonorLineup> {

}
