package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketSeriesForfeitReq {
    private Long seriesId;
    private Long forfeitEventTeamId;
    private String reason;
}
