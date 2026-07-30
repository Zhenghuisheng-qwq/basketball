package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MatchDataSubmissionListVo {
    private Long submissionId;
    private String submissionNo;
    private Long matchId;
    private String matchNo;
    private Long seasonId;
    private String seasonName;
    private Long stageId;
    private String stageName;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private String submitterTeamName;
    private String submitterMemberName;
    private Integer versionNo;
    private Integer submissionStatus;
    private String submissionStatusName;
    private String auditStatus;
    private Date submittedTime;
    private Integer materialCount;
    private Boolean riskFlag;
}
