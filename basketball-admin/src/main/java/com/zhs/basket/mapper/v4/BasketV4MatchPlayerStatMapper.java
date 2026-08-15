package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketV4MatchPlayerStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketV4MatchPlayerStat
 */
public interface BasketV4MatchPlayerStatMapper {
    BasketV4MatchPlayerStat selectById(@Param("id") Long id);
    List<BasketV4MatchPlayerStat> selectList(BasketV4MatchPlayerStat query);
    BasketV4MatchPlayerStat selectByMatchAndEventPlayer(@Param("matchId") Long matchId, @Param("eventPlayerId") Long eventPlayerId);
    List<BasketV4MatchPlayerStat> selectByDivisionId(@Param("divisionId") Long divisionId);
    int insert(BasketV4MatchPlayerStat entity);
    int updateById(BasketV4MatchPlayerStat entity);
    int softDelete(@Param("id") Long id);

    int softDeleteByMatchId(@Param("matchId") Long matchId);
}
