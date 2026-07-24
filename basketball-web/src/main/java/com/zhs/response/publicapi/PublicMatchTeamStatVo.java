package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("公开比赛球队统计")
public class PublicMatchTeamStatVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;
    private String teamName;
    private Integer points;
    private Integer rebounds;
    private Integer assists;
    private Integer steals;
    private Integer blocks;
    private Integer turnovers;
    private Integer fouls;
    private BigDecimal fieldGoalPct;
    private BigDecimal threePointPct;
    private BigDecimal freeThrowPct;
}
