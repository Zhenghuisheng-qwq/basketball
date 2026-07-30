package com.zhs.basket.schedule.strategy;

import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;

import java.util.List;

public interface ScheduleRuleStrategy {
    String ruleCode();
    String ruleVersion();
    List<MatchPlan> generate(ScheduleRuleContext context);
}
