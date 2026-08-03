package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketSeason;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketSeason
 */
public interface BasketSeasonMapper {
    BasketSeason selectById(@Param("id") Long id);
    List<BasketSeason> selectList(BasketSeason query);
    int insert(BasketSeason entity);
    int updateById(BasketSeason entity);
    int softDelete(@Param("id") Long id);
}
