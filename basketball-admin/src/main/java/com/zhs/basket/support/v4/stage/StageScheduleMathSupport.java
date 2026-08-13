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
        return computeRoundRobinStats(teamCount, 1);
    }

    static RoundRobinStats computeRoundRobinStats(int teamCount, int cycles) {
        if (teamCount < 2) {
            return new RoundRobinStats(0, 0, 0, false);
        }
        int cycleCount = cycles <= 0 ? 1 : cycles;
        boolean hasBye = teamCount % 2 == 1;
        int roundCount = (hasBye ? teamCount : teamCount - 1) * cycleCount;
        int totalMatches = teamCount * (teamCount - 1) / 2 * cycleCount;
        int matchesPerTeam = (teamCount - 1) * cycleCount;
        return new RoundRobinStats(totalMatches, matchesPerTeam, roundCount, hasBye);
    }

    static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    static int nextPowerOfTwo(int n) {
        return com.zhs.basket.support.v4.bracket.EliminationCapacity.nextPowerOfTwo(n);
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
