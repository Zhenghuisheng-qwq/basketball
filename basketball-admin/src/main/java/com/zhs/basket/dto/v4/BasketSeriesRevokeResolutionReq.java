package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketSeriesRevokeResolutionReq {
    private Long seriesId;
    private String reason;
}
