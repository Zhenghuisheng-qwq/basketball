package com.zhs.basket.req;

import lombok.Data;

@Data
public class OpenRegistrationAuditRejectReq {
    private String rejectReason;
    private String auditRemark;
}
