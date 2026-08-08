package com.zhs.basket.service.v4;

import com.zhs.basket.domain.v4.BasketMatchSeries;

import java.util.List;

/**
 * BO3/BO5 系列赛基础服务（R2-1 内部能力，不对外生成赛程）。
 */
public interface IBasketMatchSeriesService {

    BasketMatchSeries getById(Long id);

    List<BasketMatchSeries> listByStageId(Long stageId);

    List<BasketMatchSeries> listByScheduleBatchId(Long scheduleBatchId);

    List<BasketMatchSeries> listBySourceSeriesId(Long sourceSeriesId);

    Long createSeries(BasketMatchSeries series);

    void updateSeriesResult(Long seriesId, int homeWinCount, int awayWinCount,
                            Long winnerEventTeamId, String seriesStatus);

    void rebuildSeriesResult(Long seriesId);
}
