package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketManualAdvanceReq {
    private Long stageId;
    private Long eventTeamId;
    private String reason;
}
