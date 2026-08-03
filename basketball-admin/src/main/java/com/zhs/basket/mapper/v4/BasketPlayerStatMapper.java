package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPlayerStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketPlayerStat
 */
public interface BasketPlayerStatMapper {
    BasketPlayerStat selectById(@Param("id") Long id);
    List<BasketPlayerStat> selectList(BasketPlayerStat query);
    int insert(BasketPlayerStat entity);
    int updateById(BasketPlayerStat entity);
    int softDelete(@Param("id") Long id);
}
