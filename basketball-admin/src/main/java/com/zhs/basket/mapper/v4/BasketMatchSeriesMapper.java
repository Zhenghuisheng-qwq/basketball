package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchSeriesMapper {

    BasketMatchSeries selectById(@Param("id") Long id);

    BasketMatchSeries selectByIdForUpdate(@Param("id") Long id);

    List<BasketMatchSeries> selectByStageId(@Param("stageId") Long stageId);

    List<BasketMatchSeries> selectByDivisionId(@Param("divisionId") Long divisionId);

    List<BasketMatchSeries> selectCompletedByStageId(@Param("stageId") Long stageId);

    int countIncompleteByStageId(@Param("stageId") Long stageId);

    int countByStageId(@Param("stageId") Long stageId);

    List<BasketMatchSeries> selectByScheduleBatchId(@Param("scheduleBatchId") Long scheduleBatchId);

    List<BasketMatchSeries> selectBySourceSeriesId(@Param("sourceSeriesId") Long sourceSeriesId);

    int insert(BasketMatchSeries entity);

    int updateById(BasketMatchSeries entity);

    int updateSchedule(BasketMatchSeries entity);

    int updateResult(BasketMatchSeries entity);

    int updateResolution(BasketMatchSeries entity);

    int clearResolution(@Param("id") Long id);

    int softDelete(@Param("id") Long id);

    int softDeleteDraftByBatchId(@Param("scheduleBatchId") Long scheduleBatchId);
}
