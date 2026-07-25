package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatch {
    private Long id;
    private String matchNo;
    private Long seasonId;
    private Long stageId;
    private Long roundId;
    private Long groupId;
    private Long venueId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Date scheduledStartTime;
    private Date scheduledEndTime;
    private Date actualStartTime;
    private Date actualEndTime;
    private Integer matchStatus;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private Integer overtimeCount;
    private Long walkoverTeamId;
    private String cancelReason;
    private Integer dataStatus;
    private Integer resultStatus;
    private Integer scoreConfirmed;
    private Integer dataVersion;
    private Long originMatchId;
    private Integer settlementLocked;
    private Integer publicStatus;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
