package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketMatchForfeitReq {
    private Long matchId;
    private Long loserEventTeamId;
    private String reason;
}
