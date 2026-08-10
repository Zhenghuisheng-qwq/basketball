package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicMatchTeamStatItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    private String teamName;

    private Integer isWinner;

    private Integer score;

    private Integer pointsFor;

    private Integer pointsAgainst;

    private Integer fieldGoalMade;

    private Integer fieldGoalAttempt;

    private Integer threePointMade;

    private Integer threePointAttempt;

    private Integer freeThrowMade;

    private Integer freeThrowAttempt;

    private Integer rebound;

    private Integer assist;

    private Integer steal;

    private Integer block;

    private Integer turnover;

    private Integer foul;
}
