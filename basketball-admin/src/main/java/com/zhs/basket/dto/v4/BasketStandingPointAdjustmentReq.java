package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketStandingPointAdjustmentReq {
    private Long divisionId;
    private Long stageId;
    private Long eventTeamId;
    private Integer delta;
    private String reason;
    private String remark;
    private String clientRequestId;
}
