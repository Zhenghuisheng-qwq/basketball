package com.zhs.basket.req;

import lombok.Data;

import java.util.List;

@Data
public class MatchDisputeCreateReq {
    private Long matchId;
    private Long submissionId;
    private Long initiatorId;
    private Long initiatorMemberId;
    private Long initiatorPlayerId;
    private Long initiatorTeamId;
    private String initiatorType;
    private String disputeType;
    private String originalValue;
    private String expectedValue;
    private String disputeReason;
    private String evidenceFileIds;
    private List<Long> materialFileIds;
}
