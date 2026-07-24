package com.zhs.basket.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketStageStanding {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long teamId;
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
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
