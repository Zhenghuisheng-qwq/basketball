package com.zhs.basket.query;

import lombok.Data;

@Data
public class ScheduleMatchQuery {
    private Long seasonId;
    private Long stageId;
    private Integer matchStatus;
    private Integer publishStatus;
    private Long scheduleBatchId;
}
