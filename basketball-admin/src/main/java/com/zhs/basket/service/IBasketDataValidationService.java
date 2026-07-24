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
}
