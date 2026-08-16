package com.zhs.basket.service;

import com.zhs.basket.vo.PenaltyExecutionResultVo;

public interface IPenaltyExecutionService {
    PenaltyExecutionResultVo execute(Long disciplineId);

    void restoreEligibility(Long disciplineId);
}
