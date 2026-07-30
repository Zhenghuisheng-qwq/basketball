package com.zhs.basket.schedule.strategy;

import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Calendar;
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
        RegularRoundRobinScheduleGenerator generator = new RegularRoundRobinScheduleGenerator(new ScheduleTimeAllocator());
        strategy = new RegularRoundRobinV1Strategy();
        injectGenerator(strategy, generator);
    }

    private static void injectGenerator(RegularRoundRobinV1Strategy strategy, RegularRoundRobinScheduleGenerator generator) {
        try {
            var field = RegularRoundRobinV1Strategy.class.getDeclaredField("scheduleGenerator");
            field.setAccessible(true);
            field.set(strategy, generator);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
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
        assertNoDuplicatePair(plans);
        assertNoSameTimeTeamConflict(plans);
    }

    private ScheduleRuleContext baseContext(List<EventTeamScheduleSnapshot> teams) {
        ScheduleRuleContext context = new ScheduleRuleContext();
        context.setBaseDate(new GregorianCalendar(2026, Calendar.JANUARY, 6).getTime());
        context.setWeekdays(Arrays.asList(2, 4, 6));
        context.setTimeSlots(Arrays.asList("21:00", "22:00"));
        context.setSlotCapacity(12);
        context.setAllocationMode("FILL_FIRST");
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
            assertTrue(teams.add(plan.getHomeEventTeamId()));
            assertTrue(teams.add(plan.getAwayEventTeamId()));
        }
    }

    private void assertNoDuplicatePair(List<MatchPlan> plans) {
        Set<String> pairs = new HashSet<>();
        for (MatchPlan plan : plans) {
            String key = canonicalPair(plan.getHomeEventTeamId(), plan.getAwayEventTeamId());
            assertTrue(pairs.add(key));
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

    private int distinctRoundCount(List<MatchPlan> plans) {
        return (int) plans.stream().map(MatchPlan::getRoundNo).distinct().count();
    }

    private String canonicalPair(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + ":" + max;
    }

    private EventTeamScheduleSnapshot team(Long eventTeamId, Long teamId, String name) {
        EventTeamScheduleSnapshot snapshot = new EventTeamScheduleSnapshot();
        snapshot.setEventTeamId(eventTeamId);
        snapshot.setTeamId(teamId);
        snapshot.setTeamName(name);
        return snapshot;
    }
}
