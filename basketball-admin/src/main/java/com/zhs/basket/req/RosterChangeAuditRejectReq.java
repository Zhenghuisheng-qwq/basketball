package com.zhs.basket.req;

import lombok.Data;

@Data
public class RosterChangeAuditRejectReq {
    private String rejectReason;
    private String auditRemark;
}
