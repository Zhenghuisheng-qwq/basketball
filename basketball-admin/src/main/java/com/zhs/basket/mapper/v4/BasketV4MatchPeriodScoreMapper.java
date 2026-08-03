package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchPeriodScore;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchPeriodScore
 */
public interface BasketV4MatchPeriodScoreMapper {
    BasketMatchPeriodScore selectById(@Param("id") Long id);
    List<BasketMatchPeriodScore> selectList(BasketMatchPeriodScore query);
    int insert(BasketMatchPeriodScore entity);
    int updateById(BasketMatchPeriodScore entity);
    int softDelete(@Param("id") Long id);
}
