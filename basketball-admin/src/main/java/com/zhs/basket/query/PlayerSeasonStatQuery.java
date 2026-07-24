package com.zhs.basket.query;

import lombok.Data;

@Data
public class PlayerSeasonStatQuery {
    private Long seasonId;
    private Long stageId;
    private Long teamId;
    private Long playerId;
    private String keyword;
}
