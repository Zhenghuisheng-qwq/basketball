package com.zhs.basket.support.v4.leaderboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LeaderboardRanker {

    private LeaderboardRanker() {
    }

    public static List<LeaderboardSubject> rank(List<LeaderboardSubject> subjects, String sortBy, String mode,
                                                LeaderboardConfig config) {
        String metric = sortBy == null || sortBy.isBlank() ? "PTS" : sortBy.trim().toUpperCase();
        boolean average = mode == null || !"total".equalsIgnoreCase(mode);
        boolean ascending = "TOV".equals(metric);
        List<LeaderboardSubject> pool = new ArrayList<>();
        for (LeaderboardSubject subject : subjects) {
            if (!subject.isEligible()) {
                subject.setRankNo(null);
                continue;
            }
            if (!passesMetricGate(subject, metric, config)) {
                subject.setRankNo(null);
                continue;
            }
            pool.add(subject);
        }
        pool.sort(comparator(metric, average, ascending));
        int rank = 1;
        for (LeaderboardSubject subject : pool) {
            subject.setRankNo(rank++);
        }
        return pool;
    }

    public static boolean passesMetricGate(LeaderboardSubject subject, String metric, LeaderboardConfig config) {
        if (config == null || config.getMetricGates() == null) {
            return true;
        }
        LeaderboardConfig.MetricGate gate = config.getMetricGates().get(metric);
        if (gate == null || !gate.isEnabled() || gate.getMinimumAttemptsPerAppearance() <= 0) {
            return true;
        }
        int attempts = attempts(subject, metric);
        int required = gate.getMinimumAttemptsPerAppearance() * Math.max(subject.getValidGames(), 0);
        return attempts >= required && attempts > 0;
    }

    public static BigDecimal sortValue(LeaderboardSubject subject, String metric, boolean average) {
        if ("FG_PCT".equals(metric) || "THREE_PCT".equals(metric) || "FT_PCT".equals(metric)) {
            int attempts = attempts(subject, metric);
            if (attempts <= 0) {
                return null;
            }
            return BigDecimal.valueOf(made(subject, metric))
                    .divide(BigDecimal.valueOf(attempts), 6, RoundingMode.HALF_UP);
        }
        int total = switch (metric) {
            case "REB" -> subject.getRebound();
            case "AST" -> subject.getAssist();
            case "STL" -> subject.getSteal();
            case "BLK" -> subject.getBlock();
            case "TOV" -> subject.getTurnover();
            default -> subject.getPoints();
        };
        if (!average || subject.getValidGames() <= 0) {
            return BigDecimal.valueOf(total);
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(subject.getValidGames()), 6, RoundingMode.HALF_UP);
    }

    private static Comparator<LeaderboardSubject> comparator(String metric, boolean average, boolean ascending) {
        return (left, right) -> {
            BigDecimal lv = sortValue(left, metric, average);
            BigDecimal rv = sortValue(right, metric, average);
            int value = compareNullable(lv, rv, ascending);
            if (value != 0) {
                return value;
            }
            int games = Integer.compare(right.getValidGames(), left.getValidGames());
            if (games != 0) {
                return games;
            }
            int raw = Integer.compare(rawTotal(right, metric), rawTotal(left, metric));
            if (raw != 0) {
                return raw;
            }
            long leftId = left.getEventPlayerId() != null && left.getEventPlayerId() > 0
                    ? left.getEventPlayerId() : left.getEventTeamId();
            long rightId = right.getEventPlayerId() != null && right.getEventPlayerId() > 0
                    ? right.getEventPlayerId() : right.getEventTeamId();
            return Long.compare(leftId, rightId);
        };
    }

    private static int compareNullable(BigDecimal left, BigDecimal right, boolean ascending) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int value = left.compareTo(right);
        return ascending ? value : -value;
    }

    private static int rawTotal(LeaderboardSubject subject, String metric) {
        return switch (metric) {
            case "REB" -> subject.getRebound();
            case "AST" -> subject.getAssist();
            case "STL" -> subject.getSteal();
            case "BLK" -> subject.getBlock();
            case "TOV" -> subject.getTurnover();
            case "FG_PCT" -> subject.getFieldGoalMade();
            case "THREE_PCT" -> subject.getThreePointMade();
            case "FT_PCT" -> subject.getFreeThrowMade();
            default -> subject.getPoints();
        };
    }

    private static int attempts(LeaderboardSubject subject, String metric) {
        return switch (metric) {
            case "THREE_PCT" -> subject.getThreePointAttempt();
            case "FT_PCT" -> subject.getFreeThrowAttempt();
            default -> subject.getFieldGoalAttempt();
        };
    }

    private static int made(LeaderboardSubject subject, String metric) {
        return switch (metric) {
            case "THREE_PCT" -> subject.getThreePointMade();
            case "FT_PCT" -> subject.getFreeThrowMade();
            default -> subject.getFieldGoalMade();
        };
    }
}
