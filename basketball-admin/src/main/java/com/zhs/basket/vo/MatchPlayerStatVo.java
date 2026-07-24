package com.zhs.basket.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class MatchPlayerStatVo {
    private Long id;
    private Long matchId;
    private Long teamId;
    private String teamName;
    private Long playerId;
    private String playerName;
    private String playerNo;
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
    private BigDecimal fieldGoalPct;
    private Integer threePointsMade;
    private Integer threePointsAttempted;
    private BigDecimal threePointPct;
    private Integer freeThrowsMade;
    private Integer freeThrowsAttempted;
    private BigDecimal freeThrowPct;
    private Integer plusMinus;
    private BigDecimal efficiency;
    private Integer dataStatus;
    private String dataStatusName;
    private Long confirmedBy;
    private Date confirmedTime;
}
