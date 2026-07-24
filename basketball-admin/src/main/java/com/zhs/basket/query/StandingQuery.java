package com.zhs.basket.query;

import lombok.Data;

@Data
public class StandingQuery {
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long teamId;
    private String teamName;
}
