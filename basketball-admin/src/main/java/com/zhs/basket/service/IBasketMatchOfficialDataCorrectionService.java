package com.zhs.basket.service;

import com.zhs.basket.req.MatchOfficialDataCorrectionReq;

public interface IBasketMatchOfficialDataCorrectionService {

    /**
     * 修正正式比赛数据并触发重新结算（幂等由 stat_sync_record 保障）。
     *
     * @param matchId      比赛ID
     * @param req          修正数据
     * @param source       来源：DISPUTE_UPHELD / EXCEPTION_DECISION / ADMIN_REVISION
     * @param operatorId   操作人
     * @param disputeId    争议ID（可选）
     * @param reason       修正原因
     * @return revisionId
     */
    Long applyCorrection(Long matchId, MatchOfficialDataCorrectionReq req, String source,
                         Long operatorId, Long disputeId, String reason);
}
