package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketEventPlayerQuery {
    private Long seasonId;
    private Long divisionId;
    private Long eventTeamId;
    private Long sourcePlayerId;
    private String playerNameSnapshot;
}
