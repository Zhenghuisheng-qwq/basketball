package com.zhs.basket.query;

import lombok.Data;

@Data
public class MatchQuery {
    private String matchNo;
    private Long seasonId;
    private Long stageId;
    private Long roundId;
    private Long groupId;
    private Long venueId;
    private Long teamId;
    private Integer matchStatus;
    private Integer dataStatus;
    private Integer publishStatus;
}
