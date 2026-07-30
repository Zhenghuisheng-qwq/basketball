package com.zhs.basket.schedule.strategy;

import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScheduleRuleStrategyFactory {

    private final Map<String, ScheduleRuleStrategy> strategyMap = new HashMap<>();

    @Autowired
    public ScheduleRuleStrategyFactory(List<ScheduleRuleStrategy> strategies) {
        for (ScheduleRuleStrategy strategy : strategies) {
            strategyMap.put(key(strategy.ruleCode(), strategy.ruleVersion()), strategy);
        }
    }

    public ScheduleRuleStrategy getStrategy(String ruleCode, String ruleVersion) {
        ScheduleRuleStrategy strategy = strategyMap.get(key(ruleCode, ruleVersion));
        if (strategy == null) {
            throw new ServiceException("未找到赛程规则策略: " + ruleCode + " / " + ruleVersion);
        }
        return strategy;
    }

    private String key(String ruleCode, String ruleVersion) {
        return ruleCode.trim().toUpperCase() + "::" + ruleVersion.trim().toUpperCase();
    }
}
