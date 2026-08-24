package com.zhs.basket.support.penalty;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.vo.PenaltyExecutionResultVo;

public interface PenaltyExecutionStrategy {

    boolean supports(BasketDisciplineRecord record);

    PenaltyExecutionResultVo execute(BasketDisciplineRecord record);

    void restoreEligibility(BasketDisciplineRecord record);

    default void syncAfterEffective(BasketDisciplineRecord record) {
    }
}
