package com.zhs.basket.support.v4.leaderboard;

import lombok.Data;

import java.util.Date;

public final class LeaderboardFacts {

    private LeaderboardFacts() {
    }

    @Data
    public static class MatchFact {
        private Long id;
        private Long stageId;
        private Integer roundNo;
        private Long seriesId;
        private Long homeEventTeamId;
        private Long awayEventTeamId;
        private String matchStatus;
        private String publishStatus;
        private Date matchTime;
    }

    @Data
    public static class TeamBox {
        private Long matchId;
        private Long eventTeamId;
        private Integer pointsFor;
        private Integer rebound;
        private Integer assist;
        private Integer steal;
        private Integer block;
        private Integer turnover;
        private Integer isWinner;
        private Integer fieldGoalMade;
        private Integer fieldGoalAttempt;
        private Integer threePointMade;
        private Integer threePointAttempt;
        private Integer freeThrowMade;
        private Integer freeThrowAttempt;
    }

    @Data
    public static class PlayerBox {
        private Long matchId;
        private Long eventTeamId;
        private Long eventPlayerId;
        private Integer points;
        private Integer rebound;
        private Integer assist;
        private Integer steal;
        private Integer block;
        private Integer turnover;
        private Integer fieldGoalMade;
        private Integer fieldGoalAttempt;
        private Integer threePointMade;
        private Integer threePointAttempt;
        private Integer freeThrowMade;
        private Integer freeThrowAttempt;
    }
}
