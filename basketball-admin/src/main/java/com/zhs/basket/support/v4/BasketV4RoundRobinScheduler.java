package com.zhs.basket.support.v4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V4 常规赛单循环赛程算法 + 时间槽分配。
 * 时间规则：周二/周四/周六 21:00/22:00，每槽最多 6 场。
 */
public final class BasketV4RoundRobinScheduler {

    private static final String RULE_CODE = "REGULAR_ROUND_ROBIN_V1";
    private static final int[] ALLOWED_WEEKDAYS = {Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY};
    private static final int[] ALLOWED_HOURS = {21, 22};
    private static final int MAX_MATCHES_PER_SLOT = 6;

    private BasketV4RoundRobinScheduler() {}

    public static String ruleCode() {
        return RULE_CODE;
    }

    public static class Pairing {
        private final Long homeEventTeamId;
        private final Long awayEventTeamId;

        public Pairing(Long homeEventTeamId, Long awayEventTeamId) {
            this.homeEventTeamId = homeEventTeamId;
            this.awayEventTeamId = awayEventTeamId;
        }

        public Long getHomeEventTeamId() {
            return homeEventTeamId;
        }

        public Long getAwayEventTeamId() {
            return awayEventTeamId;
        }
    }

    public static class Round {
        private final int roundNo;
        private final List<Pairing> pairings;

        public Round(int roundNo, List<Pairing> pairings) {
            this.roundNo = roundNo;
            this.pairings = pairings;
        }

        public int getRoundNo() {
            return roundNo;
        }

        public List<Pairing> getPairings() {
            return pairings;
        }
    }

    /**
     * 标准循环赛圈算法：奇数队加 BYE 轮空，不生成 BYE 对阵比赛。
     */
    public static List<Round> generateRoundRobin(List<Long> teamIds) {
        if (teamIds == null || teamIds.size() < 2) {
            throw new IllegalArgumentException("至少需要 2 支球队");
        }
        List<Long> teams = new ArrayList<>(teamIds);
        if (teams.size() % 2 == 1) {
            teams.add(null);
        }
        int n = teams.size();
        int roundCount = n - 1;
        List<Long> rotation = new ArrayList<>(teams);
        List<Round> rounds = new ArrayList<>();

        for (int r = 0; r < roundCount; r++) {
            List<Pairing> pairings = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Long a = rotation.get(i);
                Long b = rotation.get(n - 1 - i);
                if (a != null && b != null) {
                    if (r % 2 == 0) {
                        pairings.add(new Pairing(a, b));
                    } else {
                        pairings.add(new Pairing(b, a));
                    }
                }
            }
            rounds.add(new Round(r + 1, pairings));
            if (n > 2) {
                Long last = rotation.remove(rotation.size() - 1);
                rotation.add(1, last);
            }
        }
        return rounds;
    }

    public static int countMatches(List<Round> rounds) {
        int total = 0;
        for (Round round : rounds) {
            total += round.getPairings().size();
        }
        return total;
    }

    public static String buildMatchNo(Long divisionId, int roundNo, int seqInRound) {
        return String.format("V4-%d-%d-%02d", divisionId, roundNo, seqInRound);
    }

    /**
     * 从 matchStartTime 起向后找第一个合法时间槽；槽满则顺延。
     */
    public static class TimeSlotAllocator {
        private final Map<String, Integer> slotUsage = new HashMap<>();
        private final Date startFrom;

        public TimeSlotAllocator(Date startFrom) {
            this.startFrom = startFrom != null ? startFrom : new Date();
        }

        public Date allocate() {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startFrom);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            for (int dayOffset = 0; dayOffset < 366 * 2; dayOffset++) {
                if (dayOffset > 0) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                if (!isAllowedWeekday(dow)) {
                    continue;
                }
                for (int hour : ALLOWED_HOURS) {
                    Calendar slotCal = (Calendar) cal.clone();
                    slotCal.set(Calendar.HOUR_OF_DAY, hour);
                    slotCal.set(Calendar.MINUTE, 0);
                    slotCal.set(Calendar.SECOND, 0);
                    slotCal.set(Calendar.MILLISECOND, 0);
                    Date slotTime = slotCal.getTime();
                    if (slotTime.before(startFrom)) {
                        continue;
                    }
                    String key = slotKey(slotCal);
                    int used = slotUsage.getOrDefault(key, 0);
                    if (used < MAX_MATCHES_PER_SLOT) {
                        slotUsage.put(key, used + 1);
                        return slotTime;
                    }
                }
            }
            throw new IllegalStateException("无法在合理范围内分配比赛时间槽");
        }

        private static boolean isAllowedWeekday(int dayOfWeek) {
            for (int allowed : ALLOWED_WEEKDAYS) {
                if (allowed == dayOfWeek) {
                    return true;
                }
            }
            return false;
        }

        private static String slotKey(Calendar cal) {
            return String.format("%04d-%02d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY));
        }
    }

    public static List<Long> sortedTeamIds(List<Long> teamIds) {
        List<Long> copy = new ArrayList<>(teamIds);
        Collections.sort(copy);
        return copy;
    }
}
