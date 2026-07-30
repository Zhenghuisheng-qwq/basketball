package com.zhs.basket.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MatchDataSubmissionTeamStatVo {
    private Long teamId;
    private String teamName;
    private Long opponentTeamId;
    private String opponentTeamName;
    private String sideType;
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
    private BigDecimal fieldGoalPct;
    private Integer threePointsMade;
    private Integer threePointsAttempted;
    private BigDecimal threePointPct;
    private Integer freeThrowsMade;
    private Integer freeThrowsAttempted;
    private BigDecimal freeThrowPct;
    private BigDecimal possessions;
    private BigDecimal efficiency;
}
