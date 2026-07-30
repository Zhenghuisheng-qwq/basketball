package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchExceptionApplication {
    private Long id;
    private String applicationNo;
    private Long matchId;
    private Long seasonId;
    private Long stageId;
    private String platformCode;
    private Long applicantMemberId;
    private Long applicantPlayerId;
    private Long applicantTeamId;
    private Long opponentTeamId;
    private String exceptionType;
    private String exceptionReason;
    private String applicationStatus;
    private String decisionType;
    private String decisionResult;
    private Integer decisionScoreHome;
    private Integer decisionScoreAway;
    private Long winnerTeamId;
    private Long walkoverTeamId;
    private Long processedBy;
    private String processedByName;
    private Date processedTime;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
