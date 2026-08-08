package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.BasketMatchSeriesDetailVo;

import java.util.List;

public interface IBasketMatchSeriesDisplayService {

    BasketMatchSeriesDetailVo getDetail(Long seriesId);

    List<BasketMatchSeriesDetailVo> listByStageId(Long stageId);

    BasketMatchSeriesDetailVo getContextByMatchId(Long matchId);
}
