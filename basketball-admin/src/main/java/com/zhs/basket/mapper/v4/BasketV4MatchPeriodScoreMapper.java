package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketV4MatchPeriodScore;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketV4MatchPeriodScore
 */
public interface BasketV4MatchPeriodScoreMapper {
    BasketV4MatchPeriodScore selectById(@Param("id") Long id);
    List<BasketV4MatchPeriodScore> selectList(BasketV4MatchPeriodScore query);
    int insert(BasketV4MatchPeriodScore entity);
    int updateById(BasketV4MatchPeriodScore entity);
    int softDelete(@Param("id") Long id);
    List<BasketV4MatchPeriodScore> selectByMatchId(@Param("matchId") Long matchId);
    int softDeleteByMatchId(@Param("matchId") Long matchId);
}
