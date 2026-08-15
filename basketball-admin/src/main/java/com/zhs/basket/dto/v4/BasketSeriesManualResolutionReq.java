package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketSeriesManualResolutionReq {
    private Long seriesId;
    private Long winnerEventTeamId;
    private String decisionType;
    private String reason;
}
