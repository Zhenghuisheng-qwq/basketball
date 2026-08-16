package com.zhs.basket.support.penalty;

import com.zhs.basket.domain.BasketDisciplineRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PenaltyExecutionStrategyFactory {

    @Autowired
    private List<PenaltyExecutionStrategy> strategies;

    public PenaltyExecutionStrategy find(BasketDisciplineRecord record) {
        if (record == null || strategies == null) {
            return null;
        }
        for (PenaltyExecutionStrategy strategy : strategies) {
            if (strategy.supports(record)) {
                return strategy;
            }
        }
        return null;
    }
}
