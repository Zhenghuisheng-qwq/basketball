package com.zhs.support.v4;

import java.util.List;

/**
 * 截图识别结果。球员能对上名单时带 eventPlayerId，对不上时只留图上的名字，交给页面手改。
 */
public record StatsScreenshotResult(
        int homeScore,
        int awayScore,
        List<PeriodScore> periods,
        List<PlayerStat> homePlayers,
        List<PlayerStat> awayPlayers,
        int unmatchedCount,
        List<String> warnings
) {
    public record PeriodScore(int periodNo, int homeScore, int awayScore) {
    }

    public record PlayerStat(
            Long eventPlayerId,
            String recognizedName,
            boolean matched,
            int points,
            int rebound,
            int assist,
            int steal,
            int block,
            int turnover,
            int foul,
            int fieldGoalMade,
            int fieldGoalAttempt,
            int threePointMade,
            int threePointAttempt,
            int freeThrowMade,
            int freeThrowAttempt
    ) {
    }
}
