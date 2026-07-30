package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataDispute {
    private Long id;
    private String disputeNo;
    private Long matchId;
    private Long submissionId;
    private String initiatorType;
    private Long initiatorId;
    private Long initiatorMemberId;
    private Long initiatorPlayerId;
    private Long initiatorTeamId;
    private String disputeType;
    private String originalValue;
    private String expectedValue;
    private String disputeReason;
    private String evidenceFileIds;
    private String disputeStatus;
    private Date disputeDeadlineTime;
    private Long processedBy;
    private String processedByName;
    private Date processedTime;
    private String processResult;
    private String rejectReason;
    private String correctionType;
    private Long handlerId;
    private Date handleTime;
    private String handleResult;
    private String handleRemark;
    private Long revisionId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
