package com.zhs.basket.service.v4;

public interface IBasketV4StatsSettlementService {
    void settleBySubmission(Long submissionId);

    void rebuildDivisionStats(Long divisionId);
}
