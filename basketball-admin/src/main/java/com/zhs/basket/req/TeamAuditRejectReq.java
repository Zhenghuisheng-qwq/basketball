package com.zhs.basket.req;

import lombok.Data;

@Data
public class TeamAuditRejectReq {
    private String rejectReason;
    private String auditRemark;
}
