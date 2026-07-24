package com.zhs.basket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TeamSeasonStatListVo {
    private Long id;
    private Long seasonId;
    private String seasonName;
    private Long stageId;
    private String stageName;
    private Long teamId;
    private String teamNo;
    private String teamName;
    private String shortName;
    private String logoUrl;
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
}
