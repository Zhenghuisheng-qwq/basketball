package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.vo.v4.BasketV4MatchListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketV4MatchMapper {
    BasketV4Match selectById(@Param("id") Long id);

    BasketV4Match selectByIdForUpdate(@Param("id") Long id);

    List<BasketV4Match> selectList(BasketV4Match query);

    List<BasketV4MatchListVo> selectVoList(BasketV4MatchQuery query);

    int countByBatchId(@Param("scheduleBatchId") Long scheduleBatchId);

    int insert(BasketV4Match entity);

    int updateById(BasketV4Match entity);

    int clearOfficialResult(@Param("id") Long id, @Param("matchStatus") String matchStatus);

    int softDelete(@Param("id") Long id);

    int softDeleteDraftByBatchId(@Param("scheduleBatchId") Long scheduleBatchId);

    int publishByBatchId(@Param("scheduleBatchId") Long scheduleBatchId);

    int unpublishByBatchId(@Param("scheduleBatchId") Long scheduleBatchId);

    int updateSchedule(BasketV4Match entity);

    List<BasketV4Match> selectFinishedPublishedByDivisionId(@Param("divisionId") Long divisionId);

    int countByDivisionId(@Param("divisionId") Long divisionId);

    int countStartedByDivisionId(@Param("divisionId") Long divisionId);

    int softDeleteUnstartedByDivisionId(@Param("divisionId") Long divisionId);

    int countByDivisionIdAndStageCode(@Param("divisionId") Long divisionId, @Param("stageCode") String stageCode);

    List<BasketV4Match> selectByStageId(@Param("stageId") Long stageId);

    List<BasketV4Match> selectByDivisionAndEventTeam(@Param("divisionId") Long divisionId,
                                                    @Param("eventTeamId") Long eventTeamId);

    List<BasketV4Match> selectByStageAndRound(@Param("stageId") Long stageId, @Param("roundNo") Integer roundNo);

    int countFinishedByStageId(@Param("stageId") Long stageId);

    int countPendingByStageId(@Param("stageId") Long stageId);

    List<BasketV4Match> selectFinishedByStageId(@Param("stageId") Long stageId);

    List<BasketV4Match> selectBySeriesId(@Param("seriesId") Long seriesId);

    List<BasketV4Match> selectEffectiveBySeriesId(@Param("seriesId") Long seriesId);
}
