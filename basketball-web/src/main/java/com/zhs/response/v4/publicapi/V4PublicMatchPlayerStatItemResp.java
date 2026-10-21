package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class V4PublicMatchPlayerStatItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventPlayerId;

    /** 永久球员 ID，公开 /player/{id} 路由必须使用此字段 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourcePlayerId;

    private String playerName;

    private String avatarUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    private String teamName;

    private Integer points;

    private Integer rebound;

    private Integer assist;

    private Integer steal;

    private Integer block;

    private Integer turnover;

    private Integer foul;

    private Integer fieldGoalMade;

    private Integer fieldGoalAttempt;

    private Integer threePointMade;

    private Integer threePointAttempt;

    private Integer freeThrowMade;

    private Integer freeThrowAttempt;

    private BigDecimal minutes;
}
