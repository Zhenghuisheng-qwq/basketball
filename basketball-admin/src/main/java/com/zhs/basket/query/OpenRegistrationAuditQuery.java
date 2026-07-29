package com.zhs.basket.query;

import lombok.Data;

@Data
public class OpenRegistrationAuditQuery {
    private Long seasonId;
    private String registrationNo;
    private Integer registrationStatus;
    /** 审核筛选阶段：PENDING/APPROVED/REJECTED/CANCELLED */
    private String auditStage;
}
