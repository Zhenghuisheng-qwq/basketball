package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketAdminBoxScoreReq {
    private Long matchId;
    private String reason;
    private List<PlayerRow> homePlayers;
    private List<PlayerRow> awayPlayers;
    private List<PeriodRow> periods;

    @Data
    public static class PlayerRow {
        private Long eventPlayerId;
        private Integer points;
        private Integer rebound;
        private Integer assist;
        private Integer steal;
        private Integer block;
        private Integer turnover;
        private Integer foul;
        private Integer fieldGoalMade;
        private Integer fieldGoalAttempt;
        private Integer threePointMade;
        private Integer threePointAttempt;
        private Integer freeThrowMade;
        private Integer freeThrowAttempt;
    }

    @Data
    public static class PeriodRow {
        private Integer periodNo;
        private Integer homeScore;
        private Integer awayScore;
    }
}
