package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketV4MatchTeamStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketV4MatchTeamStat
 */
public interface BasketV4MatchTeamStatMapper {
    BasketV4MatchTeamStat selectById(@Param("id") Long id);
    List<BasketV4MatchTeamStat> selectList(BasketV4MatchTeamStat query);
    BasketV4MatchTeamStat selectByMatchAndEventTeam(@Param("matchId") Long matchId, @Param("eventTeamId") Long eventTeamId);
    List<BasketV4MatchTeamStat> selectByDivisionId(@Param("divisionId") Long divisionId);
    int insert(BasketV4MatchTeamStat entity);
    int updateById(BasketV4MatchTeamStat entity);
    int softDelete(@Param("id") Long id);

    int softDeleteByMatchId(@Param("matchId") Long matchId);

    int deleteTombstonesByMatchId(@Param("matchId") Long matchId);

    int countByDivisionId(@Param("divisionId") Long divisionId);
}
