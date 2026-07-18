package com.zhs.dao;


import com.zhs.model.BasketPlayerSummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球员统计汇总表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketPlayerSummaryDao extends BaseMapper<BasketPlayerSummary> {

}
