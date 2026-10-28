package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionStageQualificationMapper {
    List<BasketDivisionStageQualification> selectPreviewByStageId(@Param("stageId") Long stageId);

    List<BasketDivisionStageQualification> selectConfirmedByStageId(@Param("stageId") Long stageId);

    List<BasketDivisionStageQualification> selectConfirmedChampionsBySeasonId(@Param("seasonId") Long seasonId);

    int insert(BasketDivisionStageQualification entity);

    int batchInsert(@Param("list") List<BasketDivisionStageQualification> list);

    int updateStatus(BasketDivisionStageQualification entity);

    int softDeletePreviewByStageId(@Param("stageId") Long stageId);

    int countByStageIdAndStatus(@Param("stageId") Long stageId, @Param("qualificationStatus") String qualificationStatus);

    int countConfirmedByDivisionId(@Param("divisionId") Long divisionId);

    int revertConfirmedToPreviewByStageId(@Param("stageId") Long stageId);

    BasketDivisionStageQualification selectActiveBySourceSeriesId(@Param("sourceSeriesId") Long sourceSeriesId);

    BasketDivisionStageQualification selectActiveByStageAndTeamAndType(@Param("stageId") Long stageId,
                                                                        @Param("eventTeamId") Long eventTeamId,
                                                                        @Param("qualificationType") String qualificationType);

    int softDeleteActiveBySourceSeriesId(@Param("sourceSeriesId") Long sourceSeriesId);

    int softDeletePreviewByStageAndEventTeam(@Param("stageId") Long stageId, @Param("eventTeamId") Long eventTeamId);
}
