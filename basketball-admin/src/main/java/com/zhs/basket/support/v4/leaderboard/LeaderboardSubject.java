package com.zhs.basket.support.v4.leaderboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class LeaderboardSubject {

    public static final String TEAM = "TEAM";
    public static final String PLAYER = "PLAYER";

    private String subjectType;
    private Long eventTeamId;
    private Long eventPlayerId;
    private int validGames;
    private int playedRounds;
    private int playedSeries;
    private BigDecimal dueFixtureRate;
    private int requiredGames;
    private boolean eligible;
    private List<String> failedConditions = new ArrayList<>();
    private int points;
    private int rebound;
    private int assist;
    private int steal;
    private int block;
    private int turnover;
    private int wins;
    private int losses;
    private int fieldGoalMade;
    private int fieldGoalAttempt;
    private int threePointMade;
    private int threePointAttempt;
    private int freeThrowMade;
    private int freeThrowAttempt;
    private Integer rankNo;

    public long playerKey() {
        return eventPlayerId == null ? 0L : eventPlayerId;
    }
}
