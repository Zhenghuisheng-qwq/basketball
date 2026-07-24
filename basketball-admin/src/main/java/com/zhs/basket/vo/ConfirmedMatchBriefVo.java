package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ConfirmedMatchBriefVo {
    private Long matchId;
    private String matchNo;
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private Long walkoverTeamId;
    private Integer matchStatus;
    private Integer dataStatus;
    private Date scheduledStartTime;
    private Date actualEndTime;
    private String homeResultType;
    private String awayResultType;
}
