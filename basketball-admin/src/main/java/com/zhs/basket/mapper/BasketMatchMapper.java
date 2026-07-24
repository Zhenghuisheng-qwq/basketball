package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.query.MatchQuery;
import com.zhs.basket.vo.MatchDetailVo;
import com.zhs.basket.vo.MatchListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketMatchMapper {
    int countByStageId(Long stageId);
    int countByRoundId(Long roundId);
    int countByGroupId(Long groupId);
    int countByGroupAndTeam(@Param("groupId") Long groupId, @Param("teamId") Long teamId);

    List<MatchListVo> selectList(MatchQuery query);
    MatchDetailVo selectDetailById(Long id);
    BasketMatch selectById(Long id);
    int insert(BasketMatch match);
    int updateById(BasketMatch match);
    int softDelete(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);
    int updateSchedule(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("venueId") Long venueId, @Param("scheduledStartTime") Date scheduledStartTime,
            @Param("scheduledEndTime") Date scheduledEndTime, @Param("toStatus") Integer toStatus);
    int updateCancel(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("cancelReason") String cancelReason);
    int updateStart(@Param("id") Long id, @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus, @Param("actualStartTime") Date actualStartTime);
    int updateScores(@Param("id") Long id, @Param("homeScore") Integer homeScore, @Param("awayScore") Integer awayScore,
            @Param("winnerTeamId") Long winnerTeamId, @Param("overtimeCount") Integer overtimeCount,
            @Param("walkoverTeamId") Long walkoverTeamId, @Param("matchStatus") Integer matchStatus,
            @Param("dataStatus") Integer dataStatus, @Param("actualEndTime") Date actualEndTime);
    int updateDataStatus(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);
    int countTeamScheduleConflict(@Param("excludeMatchId") Long excludeMatchId, @Param("teamId") Long teamId,
            @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    int countVenueScheduleConflict(@Param("excludeMatchId") Long excludeMatchId, @Param("venueId") Long venueId,
            @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    int countRosterByMatchId(Long matchId);
    int countPeriodScoreByMatchId(Long matchId);
    int countTeamStatByMatchId(Long matchId);
    int countPlayerStatByMatchId(Long matchId);
}
