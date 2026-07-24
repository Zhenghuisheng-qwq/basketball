package com.zhs.basket.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketMatchPlayerStat {
    private Long id;
    private Long matchId;
    private Long seasonId;
    private Long stageId;
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
    private BigDecimal fieldGoalPct;
    private Integer threePointsMade;
    private Integer threePointsAttempted;
    private BigDecimal threePointPct;
    private Integer freeThrowsMade;
    private Integer freeThrowsAttempted;
    private BigDecimal freeThrowPct;
    private Integer plusMinus;
    private BigDecimal efficiency;
    private Integer doubleDouble;
    private Integer tripleDouble;
    private Integer dataStatus;
    private Long confirmedBy;
    private Date confirmedTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
