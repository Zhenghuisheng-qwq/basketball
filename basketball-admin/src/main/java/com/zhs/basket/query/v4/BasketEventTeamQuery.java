package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketEventTeamQuery {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long sourceTeamId;
    private String teamNameSnapshot;
}
