package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ScheduleMatchDetailVo {
    private Long id;
    private String matchNo;
    private Long scheduleBatchId;
    private Long seasonId;
    private String seasonName;
    private String tournamentName;
    private String platformCode;
    private Long stageId;
    private String stageName;
    private Integer roundNo;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private String homeTeamName;
    private String awayTeamName;
    private Date scheduledStartTime;
    private Integer matchStatus;
    private String matchStatusName;
    private Integer publishStatus;
    private String publishStatusName;
    private Integer homeScore;
    private Integer awayScore;
    private String ruleCode;
    private String ruleVersion;
    private String scheduleRemark;
    private Integer dataAbnormal;
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canSwap;
    private Boolean canScore;
}
