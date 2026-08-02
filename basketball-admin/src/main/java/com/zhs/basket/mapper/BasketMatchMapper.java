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

    BasketMatch selectByIdForUpdate(Long id);
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
    int updateScoreConfirmed(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("scoreConfirmed") Integer scoreConfirmed);
    int resetScoreConfirmed(@Param("id") Long id);
    int updateResultMeta(@Param("id") Long id, @Param("resultStatus") Integer resultStatus,
            @Param("dataVersion") Integer dataVersion, @Param("publicStatus") Integer publicStatus);
    int incrementDataVersion(@Param("id") Long id, @Param("fromVersion") Integer fromVersion);
    int countTeamScheduleConflict(@Param("excludeMatchId") Long excludeMatchId, @Param("teamId") Long teamId,
            @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    int countVenueScheduleConflict(@Param("excludeMatchId") Long excludeMatchId, @Param("venueId") Long venueId,
            @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    int countRosterByMatchId(Long matchId);
    int countPeriodScoreByMatchId(Long matchId);
    int countTeamStatByMatchId(Long matchId);
    int countPlayerStatByMatchId(Long matchId);

    int countBySeasonId(@Param("seasonId") Long seasonId);
    int countPublishedBySeasonId(@Param("seasonId") Long seasonId);
    /** 可安全删除/重新生成的比赛数（草稿或未下架且未结束） */
    int countDeletableDraftBySeasonId(@Param("seasonId") Long seasonId);
    /** 当前有效赛程（已发布或草稿，不含已下架） */
    int countActiveValidScheduleBySeasonId(@Param("seasonId") Long seasonId);
    /** 已下架且可清理的赛程比赛数 */
    int countUnpublishedScheduleBySeasonId(@Param("seasonId") Long seasonId);
    /** 未发布但不可普通删除的历史脏数据（已结束/已取消/已录入等） */
    int countLegacyUnpublishedBySeasonId(@Param("seasonId") Long seasonId);
    int countEventTeamPair(@Param("seasonId") Long seasonId, @Param("homeEventTeamId") Long homeEventTeamId,
            @Param("awayEventTeamId") Long awayEventTeamId, @Param("excludeMatchId") Long excludeMatchId);
    int countEventTeamTimeConflict(@Param("excludeMatchId") Long excludeMatchId, @Param("eventTeamId") Long eventTeamId,
            @Param("matchTime") Date matchTime);
    int countEventTeamDayConflict(@Param("excludeMatchId") Long excludeMatchId, @Param("eventTeamId") Long eventTeamId,
            @Param("seasonId") Long seasonId, @Param("matchDayStart") Date matchDayStart,
            @Param("matchDayEnd") Date matchDayEnd);
    int softDeleteDraftMatch(@Param("id") Long id);
    com.zhs.basket.vo.ScheduleMatchDetailVo selectScheduleDetailById(@Param("id") Long id);
    int insertScheduleMatch(BasketMatch match);
    int updateScheduleMatch(BasketMatch match);
    int updatePublishStatusBySeasonId(@Param("seasonId") Long seasonId, @Param("publishStatus") Integer publishStatus,
            @Param("publicStatus") Integer publicStatus);
    int updatePublishStatusByBatchId(@Param("batchId") Long batchId, @Param("publishStatus") Integer publishStatus,
            @Param("publicStatus") Integer publicStatus);
    int softDeleteDraftBySeasonId(@Param("seasonId") Long seasonId);
    int updateScheduleScore(@Param("id") Long id, @Param("homeScore") Integer homeScore, @Param("awayScore") Integer awayScore,
            @Param("winnerTeamId") Long winnerTeamId, @Param("winnerEventTeamId") Long winnerEventTeamId,
            @Param("matchStatus") Integer matchStatus, @Param("dataStatus") Integer dataStatus,
            @Param("actualEndTime") Date actualEndTime);
    List<com.zhs.basket.vo.ScheduleMatchListVo> selectScheduleList(com.zhs.basket.query.ScheduleMatchQuery query);
    List<BasketMatch> selectByBatchId(@Param("batchId") Long batchId);

    List<Long> selectIdsEligibleForAutoFinish(@Param("now") Date now, @Param("limit") int limit);

    int autoFinishBySchedule(@Param("id") Long id, @Param("scheduledStatus") Integer scheduledStatus,
            @Param("finishedStatus") Integer finishedStatus, @Param("draftDataStatus") Integer draftDataStatus,
            @Param("now") Date now);
}
