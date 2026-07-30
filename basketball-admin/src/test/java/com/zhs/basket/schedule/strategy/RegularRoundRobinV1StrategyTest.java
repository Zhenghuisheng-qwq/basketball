package com.zhs.basket.schedule.strategy;

import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegularRoundRobinV1StrategyTest {

    private RegularRoundRobinV1Strategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RegularRoundRobinV1Strategy();
        strategy.timeAllocator = new ScheduleTimeAllocator();
    }

    @Test
    void generate_threeTeams_shouldCreateThreeMatches() {
        ScheduleRuleContext context = baseContext(Arrays.asList(
                team(1L, 101L, "A"),
                team(2L, 102L, "B"),
                team(3L, 103L, "C")));

        List<MatchPlan> plans = strategy.generate(context);

        assertEquals(3, plans.size());
        assertRoundRobinIntegrity(plans, 3);
    }

    @Test
    void generate_fourTeams_shouldCreateSixMatchesWithThreeRounds() {
        List<EventTeamScheduleSnapshot> teams = Arrays.asList(
                team(1L, 101L, "A"),
                team(2L, 102L, "B"),
                team(3L, 103L, "C"),
                team(4L, 104L, "D"));
        ScheduleRuleContext context = baseContext(teams);

        List<MatchPlan> plans = strategy.generate(context);

        assertEquals(6, plans.size());
        assertRoundRobinIntegrity(plans, 4);
        assertEquals(3, distinctRoundCount(plans));
        assertEachRoundAtMostOneMatchPerTeam(plans);

        Map<Long, Integer> matchCountByTeam = countMatchesByEventTeam(plans);
        assertEquals(4, matchCountByTeam.size());
        matchCountByTeam.values().forEach(count -> assertEquals(3, count));

        assertNoDuplicatePair(plans);
        assertNoSameTimeTeamConflict(plans);

        // 4 队单循环标准结构：3 轮，每轮 2 场
        Map<Integer, Long> matchesPerRound = plans.stream()
                .collect(java.util.stream.Collectors.groupingBy(MatchPlan::getRoundNo, java.util.stream.Collectors.counting()));
        assertEquals(3, matchesPerRound.size());
        matchesPerRound.values().forEach(count -> assertEquals(2L, count));

        printFourTeamScheduleReport(teams, plans);
    }

    private ScheduleRuleContext baseContext(List<EventTeamScheduleSnapshot> teams) {
        ScheduleRuleContext context = new ScheduleRuleContext();
        context.setBaseDate(new GregorianCalendar(2026, Calendar.JANUARY, 5).getTime());
        context.setWeekdays(Arrays.asList(1, 3));
        context.setTimeSlots(Arrays.asList("18:00", "19:00"));
        context.setEventTeams(teams);
        return context;
    }

    private void assertRoundRobinIntegrity(List<MatchPlan> plans, int teamCount) {
        int expectedMatches = teamCount * (teamCount - 1) / 2;
        assertEquals(expectedMatches, plans.size());
        for (MatchPlan plan : plans) {
            assertNotNull(plan.getMatchTime());
            assertNotEquals(plan.getHomeEventTeamId(), plan.getAwayEventTeamId());
            assertNotNull(plan.getRoundNo());
            assertTrue(plan.getRoundNo() > 0);
        }
    }

    private void assertEachRoundAtMostOneMatchPerTeam(List<MatchPlan> plans) {
        Map<Integer, Set<Long>> teamsByRound = new HashMap<>();
        for (MatchPlan plan : plans) {
            Set<Long> teams = teamsByRound.computeIfAbsent(plan.getRoundNo(), key -> new HashSet<>());
            assertTrue(teams.add(plan.getHomeEventTeamId()), "同一轮次出现重复球队: " + plan.getRoundNo());
            assertTrue(teams.add(plan.getAwayEventTeamId()), "同一轮次出现重复球队: " + plan.getRoundNo());
        }
    }

    private void assertNoDuplicatePair(List<MatchPlan> plans) {
        Set<String> pairs = new HashSet<>();
        for (MatchPlan plan : plans) {
            String key = canonicalPair(plan.getHomeEventTeamId(), plan.getAwayEventTeamId());
            assertTrue(pairs.add(key), "存在重复对阵: " + key);
        }
    }

    private void assertNoSameTimeTeamConflict(List<MatchPlan> plans) {
        Map<Long, Set<Long>> teamsByTime = new HashMap<>();
        for (MatchPlan plan : plans) {
            long timeKey = plan.getMatchTime().getTime();
            Set<Long> teams = teamsByTime.computeIfAbsent(timeKey, key -> new HashSet<>());
            assertTrue(teams.add(plan.getHomeEventTeamId()));
            assertTrue(teams.add(plan.getAwayEventTeamId()));
        }
    }

    private Map<Long, Integer> countMatchesByEventTeam(List<MatchPlan> plans) {
        Map<Long, Integer> counts = new HashMap<>();
        for (MatchPlan plan : plans) {
            counts.merge(plan.getHomeEventTeamId(), 1, Integer::sum);
            counts.merge(plan.getAwayEventTeamId(), 1, Integer::sum);
        }
        return counts;
    }

    private int distinctRoundCount(List<MatchPlan> plans) {
        return (int) plans.stream().map(MatchPlan::getRoundNo).distinct().count();
    }

    private String canonicalPair(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + ":" + max;
    }

    private void printFourTeamScheduleReport(List<EventTeamScheduleSnapshot> teams, List<MatchPlan> plans) {
        Map<Long, String> names = new HashMap<>();
        for (EventTeamScheduleSnapshot team : teams) {
            names.put(team.getEventTeamId(), team.getTeamName());
        }
        System.out.println("=== 4 支球队单循环赛程 ===");
        for (int round = 1; round <= distinctRoundCount(plans); round++) {
            int finalRound = round;
            System.out.println("第" + round + "轮:");
            plans.stream()
                    .filter(plan -> plan.getRoundNo() == finalRound)
                    .forEach(plan -> System.out.println("  "
                            + names.get(plan.getHomeEventTeamId()) + " vs "
                            + names.get(plan.getAwayEventTeamId())
                            + " @ " + plan.getMatchTime()));
        }
        System.out.println("每队场次: " + countMatchesByEventTeam(plans));
    }

    private EventTeamScheduleSnapshot team(Long eventTeamId, Long teamId, String name) {
        EventTeamScheduleSnapshot snapshot = new EventTeamScheduleSnapshot();
        snapshot.setEventTeamId(eventTeamId);
        snapshot.setTeamId(teamId);
        snapshot.setTeamName(name);
        return snapshot;
    }
}
