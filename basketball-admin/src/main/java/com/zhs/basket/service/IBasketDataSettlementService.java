package com.zhs.basket.service;

import com.zhs.basket.vo.SeasonSettlementSummaryVo;
import com.zhs.basket.vo.SettlementRebuildResultVo;
import com.zhs.basket.vo.SettlementValidationResultVo;

import java.util.List;

/**
 * 数据结算编排：校验、重算积分榜/赛季统计/排行榜。
 */
public interface IBasketDataSettlementService {

    List<SeasonSettlementSummaryVo> listSeasonSummaries(Long tournamentId);

    SeasonSettlementSummaryVo getSeasonSummary(Long seasonId);

    SettlementValidationResultVo validate(Long seasonId);

    SettlementRebuildResultVo rebuildSeason(Long seasonId);

    SettlementRebuildResultVo rebuildStage(Long stageId);

    SettlementRebuildResultVo rebuildGroup(Long groupId);

    SettlementRebuildResultVo rebuildMatch(Long matchId);
}
