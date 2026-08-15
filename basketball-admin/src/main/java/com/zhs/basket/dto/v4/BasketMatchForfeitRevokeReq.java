package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketMatchForfeitRevokeReq {
    private Long matchId;
    private String reason;
}
