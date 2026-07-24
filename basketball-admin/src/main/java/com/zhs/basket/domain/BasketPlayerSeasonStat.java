package com.zhs.basket.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketPlayerSeasonStat {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private Long teamId;
    private Long playerId;
    private Integer games;
    private Integer starts;
    private Integer minutesSeconds;
    private Integer points;
    private Integer rebounds;
    private Integer assists;
    private Integer steals;
    private Integer blocks;
    private Integer turnovers;
    private Integer fouls;
    private BigDecimal avgMinutes;
    private BigDecimal avgPoints;
    private BigDecimal avgRebounds;
    private BigDecimal avgAssists;
    private BigDecimal avgSteals;
    private BigDecimal avgBlocks;
    private BigDecimal fieldGoalPct;
    private BigDecimal threePointPct;
    private BigDecimal freeThrowPct;
    private BigDecimal avgEfficiency;
    private Integer doubleDoubleCount;
    private Integer tripleDoubleCount;
    private Date lastCalculatedTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
