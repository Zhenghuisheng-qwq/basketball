package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketMatchReopenSubmissionReq {
    private Long matchId;
    private Long seriesSubmissionId;
    private String reason;
}
