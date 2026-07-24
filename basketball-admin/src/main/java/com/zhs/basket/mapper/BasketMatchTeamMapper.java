package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchTeam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchTeamMapper {
    List<BasketMatchTeam> selectByMatchId(Long matchId);
    int batchInsert(@Param("list") List<BasketMatchTeam> list);
    int updateScoreAndResult(@Param("matchId") Long matchId, @Param("teamId") Long teamId,
            @Param("score") Integer score, @Param("resultType") String resultType, @Param("isWinner") Integer isWinner);
    int softDeleteByMatchId(Long matchId);
}
