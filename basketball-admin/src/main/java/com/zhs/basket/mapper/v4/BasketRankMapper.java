package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRank;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketRank
 */
public interface BasketRankMapper {
    BasketRank selectById(@Param("id") Long id);
    List<BasketRank> selectList(BasketRank query);
    int insert(BasketRank entity);
    int updateById(BasketRank entity);
    int softDelete(@Param("id") Long id);
}
