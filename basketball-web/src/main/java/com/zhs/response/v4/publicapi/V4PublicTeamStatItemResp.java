package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class V4PublicTeamStatItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    private String teamName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamLogoFileId;

    private String teamLogoUrl;

    private Integer matchCount;

    private Integer winCount;

    private Integer loseCount;

    private Integer pointsFor;

    private Integer pointsAgainst;

    private BigDecimal avgPointsFor;

    private BigDecimal avgPointsAgainst;

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
}
