package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("公开球员赛季统计")
public class PublicPlayerSeasonStatVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesId;
    private String seriesName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    private String seasonName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;
    private String divisionName;
    private String divisionStatus;
    private java.util.Date matchEndTime;
    private String platformCode;
    private String gameMode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    /** 兼容历史字段：公开球队路由使用 eventTeamId */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;
    private String teamName;
    private String teamLogoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventPlayerId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourcePlayerId;
    private String playerName;
    private String avatarUrl;

    private Integer games;
    private Integer starts;
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
    private BigDecimal avgTurnovers;
    private BigDecimal avgFouls;

    /**
     * 球员级投篮统计当前无正式数据源时可为空。
     * null = 无数据；0 = 真实 0%。
     */
    private Integer fieldGoalMade;
    private Integer fieldGoalAttempt;
    private BigDecimal fieldGoalPct;
    private Integer threePointMade;
    private Integer threePointAttempt;
    private BigDecimal threePointPct;
    private Integer freeThrowMade;
    private Integer freeThrowAttempt;
    private BigDecimal freeThrowPct;

    private BigDecimal avgEfficiency;
}
