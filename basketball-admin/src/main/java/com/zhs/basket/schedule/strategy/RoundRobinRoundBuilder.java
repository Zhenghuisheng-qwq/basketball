package com.zhs.basket.schedule.strategy;

import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;

/**
 * Berger / circle method round-robin scheduler.
 * Ensures each round every EventTeam plays at most once.
 */
public final class RoundRobinRoundBuilder {

    private static final int BYE = -1;

    private RoundRobinRoundBuilder() {
    }

    public static List<List<MatchPlan>> buildRounds(List<EventTeamScheduleSnapshot> teams) {
        if (teams == null || teams.size() < 2) {
            throw new ServiceException("正式参赛球队不足，无法生成单循环赛程");
        }
        List<Integer> rotation = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            rotation.add(i);
        }
        if (teams.size() % 2 == 1) {
            rotation.add(BYE);
        }

        int slotCount = rotation.size();
        int roundCount = slotCount - 1;
        List<List<MatchPlan>> rounds = new ArrayList<>(roundCount);
        for (int round = 0; round < roundCount; round++) {
            List<MatchPlan> roundMatches = new ArrayList<>();
            for (int i = 0; i < slotCount / 2; i++) {
                int homeIdx = rotation.get(i);
                int awayIdx = rotation.get(slotCount - 1 - i);
                if (homeIdx != BYE && awayIdx != BYE) {
                    roundMatches.add(createPlan(teams.get(homeIdx), teams.get(awayIdx)));
                }
            }
            rounds.add(roundMatches);
            rotateFixedFirst(rotation);
        }
        return rounds;
    }

    private static void rotateFixedFirst(List<Integer> rotation) {
        if (rotation.size() <= 2) {
            return;
        }
        Integer last = rotation.remove(rotation.size() - 1);
        rotation.add(1, last);
    }

    private static MatchPlan createPlan(EventTeamScheduleSnapshot home, EventTeamScheduleSnapshot away) {
        MatchPlan plan = new MatchPlan();
        plan.setHomeEventTeamId(home.getEventTeamId());
        plan.setAwayEventTeamId(away.getEventTeamId());
        plan.setHomeTeamId(home.getTeamId());
        plan.setAwayTeamId(away.getTeamId());
        plan.setHomeTeamNameSnapshot(home.getTeamName());
        plan.setAwayTeamNameSnapshot(away.getTeamName());
        plan.setHomeTeamLogoSnapshot(home.getLogoUrl());
        plan.setAwayTeamLogoSnapshot(away.getLogoUrl());
        return plan;
    }
}
