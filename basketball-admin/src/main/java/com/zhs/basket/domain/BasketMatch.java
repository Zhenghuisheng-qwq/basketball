package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatch {
    private Long id;
    private String matchNo;
    private Long scheduleBatchId;
    private Long seasonId;
    private String platformCode;
    private Long ruleId;
    private String ruleCode;
    private String ruleVersion;
    private Long stageId;
    private Long roundId;
    private Integer roundNo;
    private Long groupId;
    private Long venueId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private String homeTeamNameSnapshot;
    private String awayTeamNameSnapshot;
    private String homeTeamLogoSnapshot;
    private String awayTeamLogoSnapshot;
    private Date scheduledStartTime;
    private Date scheduledEndTime;
    private Date actualStartTime;
    private Date actualEndTime;
    private Integer matchStatus;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private Long winnerEventTeamId;
    private Integer overtimeCount;
    private Long walkoverTeamId;
    private String cancelReason;
    private String scheduleRemark;
    private Integer dataStatus;
    private Integer resultStatus;
    private Integer scoreConfirmed;
    private Integer dataVersion;
    private Long originMatchId;
    private Integer settlementLocked;
    private Integer publicStatus;
    private Integer publishStatus;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
