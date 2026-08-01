package com.zhs.basket.schedule.strategy;

import com.zhs.basket.enums.ScheduleAllocationModeEnum;
import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单循环赛程生成：动态球队数、BYE 轮空、时间槽分配。
 */
class RoundRobinScheduleGeneratorTest {

    private RegularRoundRobinScheduleGenerator generator;
    private ScheduleTimeAllocator timeAllocator;

    @BeforeEach
    void setUp() {
        timeAllocator = new ScheduleTimeAllocator();
        generator = new RegularRoundRobinScheduleGenerator(timeAllocator);
    }

    @Test
    void generate_twelveTeams_roundRobinAndTimeSlots() {
        List<MatchPlan> plans = generator.generate(cnpaContext(buildTeams(12), ScheduleAllocationModeEnum.FILL_FIRST));

        assertEquals(66, plans.size());
        assertEquals(11, distinctRoundCount(plans));
        assertRoundMatchesPerRound(plans, 6);
        assertEachRoundAtMostOneMatchPerTeam(plans);
        assertAllMatchDaysOnWeekdays(plans, 2, 4, 6);
        assertFillFirstSlotDistribution(plans, 6);
    }

    @Test
    void generate_thirteenTeams_withByeRound() {
        List<MatchPlan> plans = generator.generate(cnpaContext(buildTeams(13), ScheduleAllocationModeEnum.FILL_FIRST));

        assertEquals(78, plans.size());
        assertEquals(13, distinctRoundCount(plans));
        assertRoundMatchesPerRound(plans, 6);
        assertEachRoundAtMostOneMatchPerTeam(plans);
        assertEachTeamByeExactlyOnce(plans, 13);
        assertAllMatchDaysOnWeekdays(plans, 2, 4, 6);
    }

    @Test
    void generate_twentyFourTeams_roundRobin() {
        List<MatchPlan> plans = generator.generate(cnpaContext(buildTeams(24), ScheduleAllocationModeEnum.FILL_FIRST));

        assertEquals(276, plans.size());
        assertEquals(23, distinctRoundCount(plans));
        assertRoundMatchesPerRound(plans, 12);
        assertEachRoundAtMostOneMatchPerTeam(plans);
        assertAllMatchDaysOnWeekdays(plans, 2, 4, 6);
        assertFillFirstSlotDistribution(plans, 12);
    }

    @Test
    void generate_fortyEightTeams_timeSlotSplit() {
        List<MatchPlan> plans = generator.generate(cnpaContext(buildTeams(48), ScheduleAllocationModeEnum.FILL_FIRST));

        assertEquals(1128, plans.size());
        assertEquals(47, distinctRoundCount(plans));
        assertRoundMatchesPerRound(plans, 24);
        Map<Integer, Map<String, Long>> slotCountsByRound = countTimeSlotsByRound(plans);
        slotCountsByRound.values().forEach(slotCounts -> {
            assertEquals(12L, slotCounts.getOrDefault("21:00", 0L));
            assertEquals(12L, slotCounts.getOrDefault("22:00", 0L));
        });
    }

    @Test
    void generate_fifteenTeams_roundRobinWithByeAndLateExtraSlots() {
        List<MatchPlan> plans = generator.generate(
                cnpaContext(buildTeams(15), ScheduleAllocationModeEnum.EVEN_SPLIT_WITH_LATE_EXTRA));

        assertEquals(105, plans.size());
        assertEquals(15, distinctRoundCount(plans));
        assertRoundMatchesPerRound(plans, 7);
        assertEachRoundAtMostOneMatchPerTeam(plans);
        assertEachTeamByeExactlyOnce(plans, 15);
        assertAllMatchDaysOnWeekdays(plans, 2, 4, 6);
        Map<Integer, Map<String, Long>> slotCountsByRound = countTimeSlotsByRound(plans);
        slotCountsByRound.values().forEach(slotCounts -> {
            assertEquals(3L, slotCounts.getOrDefault("21:00", 0L));
            assertEquals(4L, slotCounts.getOrDefault("22:00", 0L));
        });
    }

    @Test
    void generate_twelveTeams_evenSplitMode() {
        List<MatchPlan> plans = generator.generate(cnpaContext(buildTeams(12), ScheduleAllocationModeEnum.EVEN_SPLIT));

        assertEquals(66, plans.size());
        Map<Integer, Map<String, Long>> slotCountsByRound = countTimeSlotsByRound(plans);
        slotCountsByRound.values().forEach(slotCounts -> {
            assertEquals(3L, slotCounts.getOrDefault("21:00", 0L));
            assertEquals(3L, slotCounts.getOrDefault("22:00", 0L));
        });
    }

    @Test
    void generate_twentyFourTeams_evenSplitMode() {
        List<MatchPlan> plans = generator.generate(cnpaContext(buildTeams(24), ScheduleAllocationModeEnum.EVEN_SPLIT));

        Map<Integer, Map<String, Long>> slotCountsByRound = countTimeSlotsByRound(plans);
        slotCountsByRound.values().forEach(slotCounts -> {
            assertEquals(6L, slotCounts.getOrDefault("21:00", 0L));
            assertEquals(6L, slotCounts.getOrDefault("22:00", 0L));
        });
    }

    private ScheduleRuleContext cnpaContext(List<EventTeamScheduleSnapshot> teams, ScheduleAllocationModeEnum mode) {
        ScheduleRuleContext context = new ScheduleRuleContext();
        context.setBaseDate(new GregorianCalendar(2026, Calendar.JANUARY, 6).getTime());
        context.setWeekdays(Arrays.asList(2, 4, 6));
        context.setTimeSlots(Arrays.asList("21:00", "22:00"));
        context.setSlotCapacity(12);
        context.setAllocationMode(mode.getCode());
        context.setEventTeams(teams);
        return context;
    }

    private List<EventTeamScheduleSnapshot> buildTeams(int count) {
        List<EventTeamScheduleSnapshot> teams = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            EventTeamScheduleSnapshot team = new EventTeamScheduleSnapshot();
            team.setEventTeamId((long) i);
            team.setTeamId(100L + i);
            team.setTeamName("Team-" + i);
            teams.add(team);
        }
        return teams;
    }

    private void assertRoundMatchesPerRound(List<MatchPlan> plans, int expectedPerRound) {
        Map<Integer, Long> byRound = plans.stream()
                .collect(Collectors.groupingBy(MatchPlan::getRoundNo, Collectors.counting()));
        byRound.values().forEach(count -> assertEquals((long) expectedPerRound, count));
    }

    private void assertEachRoundAtMostOneMatchPerTeam(List<MatchPlan> plans) {
        Map<Integer, Set<Long>> teamsByRound = new HashMap<>();
        for (MatchPlan plan : plans) {
            Set<Long> teams = teamsByRound.computeIfAbsent(plan.getRoundNo(), key -> new HashSet<>());
            assertTrue(teams.add(plan.getHomeEventTeamId()));
            assertTrue(teams.add(plan.getAwayEventTeamId()));
        }
    }

    private void assertEachTeamByeExactlyOnce(List<MatchPlan> plans, int teamCount) {
        Map<Integer, Set<Long>> teamsByRound = new HashMap<>();
        for (MatchPlan plan : plans) {
            Set<Long> teams = teamsByRound.computeIfAbsent(plan.getRoundNo(), key -> new HashSet<>());
            teams.add(plan.getHomeEventTeamId());
            teams.add(plan.getAwayEventTeamId());
        }
        Map<Long, Integer> byeCount = new HashMap<>();
        for (int round = 1; round <= distinctRoundCount(plans); round++) {
            Set<Long> playing = teamsByRound.getOrDefault(round, Set.of());
            for (long teamId = 1; teamId <= teamCount; teamId++) {
                if (!playing.contains(teamId)) {
                    byeCount.merge(teamId, 1, Integer::sum);
                }
            }
        }
        assertEquals(teamCount, byeCount.size());
        byeCount.values().forEach(count -> assertEquals(1, count));
    }

    private void assertAllMatchDaysOnWeekdays(List<MatchPlan> plans, int... weekdays) {
        Set<Integer> allowed = Arrays.stream(weekdays).boxed().collect(Collectors.toSet());
        Set<LocalDate> roundDates = new HashSet<>();
        for (MatchPlan plan : plans) {
            LocalDate date = toLocalDateTime(plan.getMatchTime()).toLocalDate();
            roundDates.add(date);
        }
        for (LocalDate date : roundDates) {
            assertTrue(allowed.contains(date.getDayOfWeek().getValue()),
                    "比赛日 " + date + " 不在允许的比赛日列表中");
        }
    }

    private void assertFillFirstSlotDistribution(List<MatchPlan> plans, int matchesPerRound) {
        Map<Integer, Map<String, Long>> slotCountsByRound = countTimeSlotsByRound(plans);
        slotCountsByRound.values().forEach(slotCounts -> {
            if (matchesPerRound <= 12) {
                assertEquals((long) matchesPerRound, slotCounts.getOrDefault("21:00", 0L));
                assertEquals(0L, slotCounts.getOrDefault("22:00", 0L));
            }
        });
    }

    private Map<Integer, Map<String, Long>> countTimeSlotsByRound(List<MatchPlan> plans) {
        Map<Integer, Map<String, Long>> result = new HashMap<>();
        for (MatchPlan plan : plans) {
            String slot = formatTimeSlot(plan.getMatchTime());
            result.computeIfAbsent(plan.getRoundNo(), key -> new HashMap<>())
                    .merge(slot, 1L, Long::sum);
        }
        return result;
    }

    private String formatTimeSlot(Date date) {
        LocalTime time = toLocalDateTime(date).toLocalTime();
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private int distinctRoundCount(List<MatchPlan> plans) {
        return (int) plans.stream().map(MatchPlan::getRoundNo).distinct().count();
    }
}
