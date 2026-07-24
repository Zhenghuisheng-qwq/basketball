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
    private Long seasonId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;
    private String teamName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    private String playerName;
    private String avatarUrl;
    private Integer games;
    private Integer starts;
    private Integer points;
    private Integer rebounds;
    private Integer assists;
    private Integer steals;
    private Integer blocks;
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
}
