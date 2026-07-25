package com.zhs.basket.query;

import lombok.Data;

@Data
public class MatchDisputeQuery {
    private Long seasonId;
    private Long matchId;
    private String disputeStatus;
    private String disputeType;
}
