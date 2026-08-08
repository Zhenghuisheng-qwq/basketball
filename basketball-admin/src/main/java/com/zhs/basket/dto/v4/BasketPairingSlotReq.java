package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketPairingSlotReq {
    private Integer bracketSlot;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
}
