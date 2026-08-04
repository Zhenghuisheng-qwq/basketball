package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketCaptainGrantQuery {
    private Long seasonId;
    private Long divisionId;
    private Long teamId;
    private Long playerId;
    private String grantStatus;
    private String teamName;
    private String playerName;
}
