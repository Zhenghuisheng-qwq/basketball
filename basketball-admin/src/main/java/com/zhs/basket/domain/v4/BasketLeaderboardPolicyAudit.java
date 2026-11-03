package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketLeaderboardPolicyAudit {
    private Long id;
    private Long policyId;
    private Long divisionId;
    private String scopeKey;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String beforeJson;
    private String afterJson;
    private String reason;
    private Date createTime;
}
