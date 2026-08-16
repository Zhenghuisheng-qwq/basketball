package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class DisciplineListVo {
    private Long id;
    private String recordNo;
    private String targetType;
    private Long seasonId;
    private String seasonName;
    private String seriesName;
    private Long teamId;
    private String teamName;
    private Long eventTeamId;
    private String eventTeamName;
    private Long playerId;
    private String playerNo;
    private String playerName;
    private Long memberId;
    private String disciplineType;
    private String penaltyCode;
    private String title;
    private String reasonText;
    private Integer disciplineStatus;
    private String recordStatus;
    private String executionStatus;
    private Integer matchSuspensionRemaining;
    private Integer pointDeduction;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Date decisionTime;
    private Date createTime;
}
