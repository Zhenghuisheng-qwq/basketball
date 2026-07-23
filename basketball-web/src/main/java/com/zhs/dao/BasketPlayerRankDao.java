package com.zhs.dao;


import com.zhs.model.BasketPlayerRank;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 球员排行榜快照表 的dao
 *
 * @author 
 * @since 2026-07-24
 */
@Mapper
@Repository
public interface BasketPlayerRankDao extends BaseMapper<BasketPlayerRank> {

}
