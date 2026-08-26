package com.zhs.basket.support.v4.stage;

import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Round;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.TimeSlotAllocator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSlotAllocatorMatchDayBoundaryTest {

    @Test
    void sixteenTeams_eachRoundOwnsOneMatchDay() {
        Map<Integer, List<Date>> byRound = allocateLikeStrategy(16);
        assertEquals(15, byRound.size());
        for (int roundNo = 1; roundNo <= 15; roundNo++) {
            List<Date> times = byRound.get(roundNo);
            assertEquals(8, times.size());
            assertEquals(1, distinctDays(times).size());
            assertEquals(6, countHour(times, 21));
            assertEquals(2, countHour(times, 22));
        }
        assertStrictRoundDayBoundary(byRound);
    }

    @Test
    void thirtyTwoTeams_roundSpansDays_nextRoundStartsAfter() {
        Map<Integer, List<Date>> byRound = allocateLikeStrategy(32);
        assertEquals(31, byRound.size());
        List<Date> round1 = byRound.get(1);
        assertEquals(16, round1.size());
        TreeSet<Integer> r1Days = distinctDays(round1);
        assertEquals(2, r1Days.size());
        assertEquals(12, countOnDay(round1, r1Days.first()));
        assertEquals(4, countOnDay(round1, r1Days.last()));

        List<Date> round2 = byRound.get(2);
        assertEquals(16, round2.size());
        assertTrue(r1Days.last() < distinctDays(round2).first());
        assertStrictRoundDayBoundary(byRound);
    }

    @Test
    void firstRoundStillStartsFromMatchStartTime() {
        Calendar start = calendarAt(2026, Calendar.AUGUST, 26, 0, 0);
        TimeSlotAllocator allocator = new TimeSlotAllocator(start.getTime());
        Date first = RoundRobinStageScheduleStrategy.nextRoundRobinMatchTime(allocator, null, 1);
        Calendar got = Calendar.getInstance();
        got.setTime(first);
        assertEquals(2026, got.get(Calendar.YEAR));
        assertEquals(Calendar.AUGUST, got.get(Calendar.MONTH));
        assertEquals(27, got.get(Calendar.DAY_OF_MONTH));
        assertEquals(21, got.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    void officialRoundOrderIsRoundNoThenPairingSequence() {
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(teamIds(16), 1);
        assertEquals(15, rounds.size());
        int expected = 1;
        for (Round round : rounds) {
            assertEquals(expected, round.getRoundNo());
            assertEquals(8, round.getPairings().size());
            expected++;
        }
        assertEquals(120, BasketV4RoundRobinScheduler.countMatches(rounds));
        assertEquals("V4-1-14-05", BasketV4RoundRobinScheduler.buildMatchNo(1L, 14, 5));
        assertEquals("V4-1-15-01", BasketV4RoundRobinScheduler.buildMatchNo(1L, 15, 1));
    }

    @Test
    void allocateWithoutAdvance_stillSharesDayAcrossRounds() {
        TimeSlotAllocator allocator = new TimeSlotAllocator(calendarAt(2026, Calendar.SEPTEMBER, 1, 0, 0).getTime());
        List<Date> firstEight = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            firstEight.add(allocator.allocate());
        }
        List<Date> nextEight = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            nextEight.add(allocator.allocate());
        }
        TreeSet<Integer> shared = distinctDays(firstEight);
        shared.retainAll(distinctDays(nextEight));
        assertEquals(1, shared.size());
    }

    private static Map<Integer, List<Date>> allocateLikeStrategy(int teamCount) {
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(teamIds(teamCount), 1);
        TimeSlotAllocator allocator = new TimeSlotAllocator(
                calendarAt(2026, Calendar.AUGUST, 26, 0, 0).getTime());
        Map<Integer, List<Date>> byRound = new LinkedHashMap<>();
        Integer previousRoundNo = null;
        for (Round round : rounds) {
            for (int i = 0; i < round.getPairings().size(); i++) {
                Date time = RoundRobinStageScheduleStrategy.nextRoundRobinMatchTime(
                        allocator, previousRoundNo, round.getRoundNo());
                previousRoundNo = round.getRoundNo();
                byRound.computeIfAbsent(round.getRoundNo(), key -> new ArrayList<>()).add(time);
            }
        }
        return byRound;
    }

    private static void assertStrictRoundDayBoundary(Map<Integer, List<Date>> byRound) {
        Integer previousMax = null;
        for (Map.Entry<Integer, List<Date>> entry : byRound.entrySet()) {
            TreeSet<Integer> days = distinctDays(entry.getValue());
            if (previousMax != null) {
                assertTrue(previousMax < days.first(),
                        "round " + entry.getKey() + " must start after previous round last day");
            }
            previousMax = days.last();
        }
    }

    private static List<Long> teamIds(int teamCount) {
        List<Long> ids = new ArrayList<>(teamCount);
        for (long i = 1; i <= teamCount; i++) {
            ids.add(i);
        }
        return ids;
    }

    private static Calendar calendarAt(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day, hour, minute, 0);
        return cal;
    }

    private static int dayKey(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) * 10000
                + (cal.get(Calendar.MONTH) + 1) * 100
                + cal.get(Calendar.DAY_OF_MONTH);
    }

    private static TreeSet<Integer> distinctDays(List<Date> times) {
        TreeSet<Integer> days = new TreeSet<>();
        for (Date time : times) {
            days.add(dayKey(time));
        }
        return days;
    }

    private static int countOnDay(List<Date> times, int day) {
        int count = 0;
        for (Date time : times) {
            if (dayKey(time) == day) {
                count++;
            }
        }
        return count;
    }

    private static int countHour(List<Date> times, int hour) {
        int count = 0;
        for (Date time : times) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(time);
            if (cal.get(Calendar.HOUR_OF_DAY) == hour) {
                count++;
            }
        }
        return count;
    }
}
