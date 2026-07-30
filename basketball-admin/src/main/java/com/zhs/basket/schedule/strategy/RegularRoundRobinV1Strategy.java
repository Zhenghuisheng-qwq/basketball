package com.zhs.basket.schedule.strategy;

import com.zhs.basket.enums.ScheduleRuleCodeEnum;
import com.zhs.basket.enums.ScheduleRuleVersionEnum;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegularRoundRobinV1Strategy implements ScheduleRuleStrategy {

    @Autowired
    private RegularRoundRobinScheduleGenerator scheduleGenerator;

    @Override
    public String ruleCode() {
        return ScheduleRuleCodeEnum.REGULAR_ROUND_ROBIN.getCode();
    }

    @Override
    public String ruleVersion() {
        return ScheduleRuleVersionEnum.V1.getCode();
    }

    @Override
    public List<MatchPlan> generate(ScheduleRuleContext context) {
        return scheduleGenerator.generate(context);
    }
}
