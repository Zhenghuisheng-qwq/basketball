package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataSubmission {
    private Long id;
    private String submissionNo;
    private Long matchId;
    private Long seasonId;
    private Long stageId;
    private Long roundId;
    private Long groupId;
    private String platformCode;
    private Long homeTeamId;
    private Long awayTeamId;
    private Long submitterTeamId;
    private Long submitterMemberId;
    private Long submitterPlayerId;
    private String submitterRole;
    private Integer versionNo;
    private Long previousSubmissionId;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private Integer submissionStatus;
    private String auditStatus;
    private Date submittedTime;
    private Date reviewedTime;
    private Long reviewedBy;
    private String reviewedByName;
    private String rejectReason;
    private String auditRemark;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
