package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchPlayerStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchPlayerStat
 */
public interface BasketV4MatchPlayerStatMapper {
    BasketMatchPlayerStat selectById(@Param("id") Long id);
    List<BasketMatchPlayerStat> selectList(BasketMatchPlayerStat query);
    int insert(BasketMatchPlayerStat entity);
    int updateById(BasketMatchPlayerStat entity);
    int softDelete(@Param("id") Long id);
}
