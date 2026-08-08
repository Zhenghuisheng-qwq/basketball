package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketPairingAdjustReq {
    private Long stageId;
    private List<BasketPairingSlotReq> slots;
    private String reason;
}
