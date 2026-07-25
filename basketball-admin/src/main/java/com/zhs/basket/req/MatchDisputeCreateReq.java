package com.zhs.basket.req;

import lombok.Data;

@Data
public class MatchDisputeCreateReq {
    private Long matchId;
    private String initiatorType;
    private String disputeType;
    private String originalValue;
    private String expectedValue;
    private String disputeReason;
    private String evidenceFileIds;
}
