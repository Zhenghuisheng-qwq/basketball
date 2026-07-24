package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("公开积分榜项")
public class PublicStandingItemVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long standingId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;
    private String stageName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;
    private String groupName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;
    private String teamName;
    private String teamLogoUrl;
    private Integer rankNo;
    private Integer matchesPlayed;
    private Integer wins;
    private Integer losses;
    private Integer pointsFor;
    private Integer pointsAgainst;
    private Integer pointDiff;
    private BigDecimal standingPoints;
    private BigDecimal winRate;
    private String streakType;
    private Integer streakCount;
    private Integer qualifiedFlag;
}
