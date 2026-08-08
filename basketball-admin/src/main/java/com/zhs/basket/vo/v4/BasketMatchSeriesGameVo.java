package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchSeriesGameVo {
    private Long matchId;
    private String matchCode;
    private Integer seriesGameNo;
    private String matchStatus;
    private String matchStatusName;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerEventTeamId;
    private Date scheduledTime;
    private String submissionStatus;
    private String submissionStatusName;
    private String auditStatus;
    private String auditStatusName;
    /** 系列赛已提前结束且本局无正式结果时为 true */
    private Boolean skipped;
    private String skippedReason;
}
