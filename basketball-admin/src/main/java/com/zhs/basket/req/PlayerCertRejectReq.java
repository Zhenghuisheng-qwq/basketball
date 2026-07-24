package com.zhs.basket.req;

import lombok.Data;

@Data
public class PlayerCertRejectReq {
    private String rejectReason;
    private String auditRemark;
}
