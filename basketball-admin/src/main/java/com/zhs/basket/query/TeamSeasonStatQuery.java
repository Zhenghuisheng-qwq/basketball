package com.zhs.basket.query;

import lombok.Data;

@Data
public class TeamSeasonStatQuery {
    private Long seasonId;
    private Long stageId;
    private Long teamId;
    private String keyword;
}
