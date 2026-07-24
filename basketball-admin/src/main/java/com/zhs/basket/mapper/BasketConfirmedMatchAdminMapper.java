package com.zhs.basket.mapper;

import com.zhs.basket.vo.ConfirmedMatchBriefVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface BasketConfirmedMatchAdminMapper {

    List<ConfirmedMatchBriefVo> selectConfirmedMatches(@Param("seasonId") Long seasonId,
                                                       @Param("stageId") Long stageId,
                                                       @Param("groupId") Long groupId);

    ConfirmedMatchBriefVo selectConfirmedMatchById(@Param("matchId") Long matchId);

    int countBySeasonAndStatuses(@Param("seasonId") Long seasonId,
                                 @Param("matchStatus") Integer matchStatus,
                                 @Param("dataStatus") Integer dataStatus);

    int countCancelled(@Param("seasonId") Long seasonId);

    int countReopened(@Param("seasonId") Long seasonId);

    int countUnfinished(@Param("seasonId") Long seasonId);

    List<Map<String, Object>> selectTeamStatAgg(@Param("seasonId") Long seasonId,
                                                @Param("stageId") Long stageId);

    List<Map<String, Object>> selectPlayerStatAgg(@Param("seasonId") Long seasonId,
                                                  @Param("stageId") Long stageId);

    int countConfirmedTeamStat(@Param("matchId") Long matchId);

    int countConfirmedPlayerStat(@Param("matchId") Long matchId);

    Integer sumPlayerPointsByMatchTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);

    Integer sumTeamPointsByMatchTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);

    int countDuplicateTeamStat(@Param("matchId") Long matchId);

    int countDuplicatePlayerStat(@Param("matchId") Long matchId);
}
