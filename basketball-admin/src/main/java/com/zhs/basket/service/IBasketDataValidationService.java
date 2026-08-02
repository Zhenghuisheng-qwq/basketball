package com.zhs.basket.service;

import com.zhs.basket.vo.SettlementValidationResultVo;

/**
 * 赛季结算前数据校验。
 */
public interface IBasketDataValidationService {

    /**
     * 校验赛季已确认比赛数据完整性与积分规则配置。
     */
    SettlementValidationResultVo validateSeason(Long seasonId);

    /**
     * 校验单场已确认比赛（审核通过自动重算时使用，不阻断于赛季内其他历史脏数据）。
     */
    SettlementValidationResultVo validateMatch(Long matchId);
}
