package com.zhs.basket.req;

import lombok.Data;

@Data
public class RegistrationAuditRejectReq {
    private String rejectReason;
    private String auditRemark;
}
