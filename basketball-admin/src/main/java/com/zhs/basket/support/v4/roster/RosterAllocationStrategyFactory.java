package com.zhs.basket.support.v4.roster;

import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RosterAllocationStrategyFactory {

    private final Map<String, RosterAllocationStrategy> strategies;

    public RosterAllocationStrategyFactory(List<RosterAllocationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(RosterAllocationStrategy::type, Function.identity()));
    }

    public RosterAllocationStrategy get(String type) {
        RosterAllocationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new ServiceException("不支持的名单分配策略: " + type);
        }
        return strategy;
    }
}
