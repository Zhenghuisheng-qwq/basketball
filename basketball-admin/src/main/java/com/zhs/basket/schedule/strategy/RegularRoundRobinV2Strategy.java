package com.zhs.basket.schedule.strategy;

import com.zhs.basket.enums.ScheduleRuleCodeEnum;
import com.zhs.basket.enums.ScheduleRuleVersionEnum;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CNPA 正式赛程规则 V2：周二/四/六，21:00/22:00，动态球队数单循环。
 */
@Component
public class RegularRoundRobinV2Strategy implements ScheduleRuleStrategy {

    @Autowired
    private RegularRoundRobinScheduleGenerator scheduleGenerator;

    @Override
    public String ruleCode() {
        return ScheduleRuleCodeEnum.REGULAR_ROUND_ROBIN.getCode();
    }

    @Override
    public String ruleVersion() {
        return ScheduleRuleVersionEnum.V2.getCode();
    }

    @Override
    public List<MatchPlan> generate(ScheduleRuleContext context) {
        return scheduleGenerator.generate(context);
    }
}
