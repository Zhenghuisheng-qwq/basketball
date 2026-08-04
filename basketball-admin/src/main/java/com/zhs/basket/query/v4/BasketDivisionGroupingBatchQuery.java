package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketDivisionGroupingBatchQuery {
    private Long divisionId;
    private Long stageId;
    private String groupingStatus;
}
