package com.zhs.basket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PlayerSeasonStatListVo {
    private Long id;
    private Long seasonId;
    private String seasonName;
    private Long stageId;
    private String stageName;
    private Long teamId;
    private String teamName;
    private Long playerId;
    private String playerNo;
    private String displayName;
    private String avatarUrl;
    private String positionCode;
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
}
