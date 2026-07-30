package com.zhs.basket.schedule.strategy;

import com.zhs.basket.enums.ScheduleRuleCodeEnum;
import com.zhs.basket.enums.ScheduleRuleVersionEnum;
import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class RegularRoundRobinV1Strategy implements ScheduleRuleStrategy {

    @Autowired
    ScheduleTimeAllocator timeAllocator;

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
        List<EventTeamScheduleSnapshot> teams = context.getEventTeams();
        if (teams == null || teams.size() < 2) {
            throw new ServiceException("正式参赛球队不足，无法生成单循环赛程");
        }
        List<List<MatchPlan>> rounds = RoundRobinRoundBuilder.buildRounds(teams);
        List<Date> roundTimes = timeAllocator.allocateTimes(rounds.size(), context);

        List<MatchPlan> result = new ArrayList<>();
        int sortNo = 1;
        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            Date roundTime = roundTimes.get(roundIndex);
            int roundNo = roundIndex + 1;
            for (MatchPlan plan : rounds.get(roundIndex)) {
                plan.setMatchTime(roundTime);
                plan.setRoundNo(roundNo);
                plan.setSortNo(sortNo++);
                result.add(plan);
            }
        }
        return result;
    }
}
