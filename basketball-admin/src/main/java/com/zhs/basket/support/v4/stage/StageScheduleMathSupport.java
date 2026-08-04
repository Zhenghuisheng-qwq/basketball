package com.zhs.basket.support.v4.stage;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 阶段赛程数量计算工具。
 */
final class StageScheduleMathSupport {

    private StageScheduleMathSupport() {}

    @Data
    @AllArgsConstructor
    static class RoundRobinStats {
        private int totalMatches;
        private int matchesPerTeam;
        private int roundCount;
        private boolean hasBye;
    }

    static RoundRobinStats computeRoundRobinStats(int teamCount) {
        if (teamCount < 2) {
            return new RoundRobinStats(0, 0, 0, false);
        }
        boolean hasBye = teamCount % 2 == 1;
        int roundCount = hasBye ? teamCount : teamCount - 1;
        int totalMatches = teamCount * (teamCount - 1) / 2;
        int matchesPerTeam = teamCount - 1;
        return new RoundRobinStats(totalMatches, matchesPerTeam, roundCount, hasBye);
    }

    static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    static int eliminationRoundCount(int teamCount) {
        if (!isPowerOfTwo(teamCount)) {
            return 0;
        }
        return Integer.numberOfTrailingZeros(teamCount);
    }

    static int eliminationMatchCount(int teamCount) {
        return teamCount - 1;
    }
}
