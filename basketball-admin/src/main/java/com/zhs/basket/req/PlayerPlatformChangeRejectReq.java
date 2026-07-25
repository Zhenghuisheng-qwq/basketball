package com.zhs.basket.req;

import lombok.Data;

@Data
public class PlayerPlatformChangeRejectReq {
    private String rejectReason;
    private String auditRemark;
}
