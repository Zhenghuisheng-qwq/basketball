package com.zhs.basket.schedule.strategy;

import com.zhs.basket.enums.ScheduleRuleCodeEnum;
import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 常规赛单循环赛程生成（Berger 轮转 + 按轮分配比赛日/时间槽）。
 */
@Component
public class RegularRoundRobinScheduleGenerator {

    @Autowired
    private ScheduleTimeAllocator timeAllocator;

    /** 供单元测试注入依赖 */
    RegularRoundRobinScheduleGenerator(ScheduleTimeAllocator timeAllocator) {
        this.timeAllocator = timeAllocator;
    }

    public RegularRoundRobinScheduleGenerator() {
    }

    public List<MatchPlan> generate(ScheduleRuleContext context) {
        List<EventTeamScheduleSnapshot> teams = context.getEventTeams();
        if (teams == null || teams.size() < 2) {
            throw new ServiceException("正式参赛球队不足，无法生成单循环赛程");
        }
        List<List<MatchPlan>> rounds = RoundRobinRoundBuilder.buildRounds(teams);
        timeAllocator.allocateMatchTimes(rounds, context);

        List<MatchPlan> result = new ArrayList<>();
        int sortNo = 1;
        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            int roundNo = roundIndex + 1;
            for (MatchPlan plan : rounds.get(roundIndex)) {
                plan.setRoundNo(roundNo);
                plan.setSortNo(sortNo++);
                result.add(plan);
            }
        }
        return result;
    }
}
