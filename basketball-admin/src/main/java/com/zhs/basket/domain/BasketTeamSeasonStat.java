package com.zhs.basket.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketTeamSeasonStat {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private Long teamId;
    private Integer games;
    private Integer wins;
    private Integer losses;
    private Integer pointsFor;
    private Integer pointsAgainst;
    private BigDecimal avgPoints;
    private BigDecimal avgPointsAgainst;
    private BigDecimal avgRebounds;
    private BigDecimal avgAssists;
    private BigDecimal avgSteals;
    private BigDecimal avgBlocks;
    private BigDecimal avgTurnovers;
    private BigDecimal fieldGoalPct;
    private BigDecimal threePointPct;
    private BigDecimal freeThrowPct;
    private BigDecimal winRate;
    private Date lastCalculatedTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
