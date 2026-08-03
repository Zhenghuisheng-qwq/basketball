package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketStanding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketStanding
 */
public interface BasketStandingMapper {
    BasketStanding selectById(@Param("id") Long id);
    List<BasketStanding> selectList(BasketStanding query);
    int insert(BasketStanding entity);
    int updateById(BasketStanding entity);
    int softDelete(@Param("id") Long id);
}
