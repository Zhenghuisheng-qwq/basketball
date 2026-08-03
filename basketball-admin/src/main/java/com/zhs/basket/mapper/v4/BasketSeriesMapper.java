package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketSeries;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketSeries
 */
public interface BasketSeriesMapper {
    BasketSeries selectById(@Param("id") Long id);
    List<BasketSeries> selectList(BasketSeries query);
    int insert(BasketSeries entity);
    int updateById(BasketSeries entity);
    int softDelete(@Param("id") Long id);
}
