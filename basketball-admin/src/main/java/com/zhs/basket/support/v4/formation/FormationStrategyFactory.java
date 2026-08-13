package com.zhs.basket.support.v4.formation;

import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormationStrategyFactory {

    private final Map<String, FormationStrategy> strategies;

    public FormationStrategyFactory(List<FormationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(FormationStrategy::type, Function.identity()));
    }

    public FormationStrategy get(String type) {
        String key = type == null || type.isEmpty() ? BalancedRandomFormationStrategy.TYPE : type;
        FormationStrategy strategy = strategies.get(key);
        if (strategy == null) {
            throw new ServiceException("\u4e0d\u652f\u6301\u7684\u7ec4\u961f\u7b97\u6cd5: " + key);
        }
        return strategy;
    }
}
