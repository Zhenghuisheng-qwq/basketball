package com.zhs.basket.req;

import lombok.Data;

import java.util.List;

@Data
public class MatchOfficialDataCorrectionReq {
    private Integer homeScore;
    private Integer awayScore;
    private List<PeriodItem> periodScores;
    private List<TeamStatItem> teamStats;
    private List<PlayerStatItem> playerStats;
    private String correctionReason;

    @Data
    public static class PeriodItem {
        private Integer periodNo;
        private String periodType;
        private Integer homeScore;
        private Integer awayScore;
    }

    @Data
    public static class TeamStatItem {
        private Long teamId;
        private Integer points;
        private Integer rebounds;
        private Integer offensiveRebounds;
        private Integer defensiveRebounds;
        private Integer assists;
        private Integer steals;
        private Integer blocks;
        private Integer turnovers;
        private Integer fouls;
        private Integer fieldGoalsMade;
        private Integer fieldGoalsAttempted;
        private Integer threePointsMade;
        private Integer threePointsAttempted;
        private Integer freeThrowsMade;
        private Integer freeThrowsAttempted;
    }

    @Data
    public static class PlayerStatItem {
        private Long teamId;
        private Long playerId;
        private Long rosterId;
        private Integer startedFlag;
        private Integer minutesSeconds;
        private Integer points;
        private Integer rebounds;
        private Integer offensiveRebounds;
        private Integer defensiveRebounds;
        private Integer assists;
        private Integer steals;
        private Integer blocks;
        private Integer turnovers;
        private Integer fouls;
        private Integer fieldGoalsMade;
        private Integer fieldGoalsAttempted;
        private Integer threePointsMade;
        private Integer threePointsAttempted;
        private Integer freeThrowsMade;
        private Integer freeThrowsAttempted;
    }
}
