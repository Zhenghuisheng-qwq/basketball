package com.zhs.basket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class StandingListVo {
    private Long id;
    private Long seasonId;
    private String seasonName;
    private Long stageId;
    private String stageName;
    private Long groupId;
    private String groupName;
    private Long teamId;
    private String teamNo;
    private String teamName;
    private String shortName;
    private String logoUrl;
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
    private Date snapshotTime;
}
