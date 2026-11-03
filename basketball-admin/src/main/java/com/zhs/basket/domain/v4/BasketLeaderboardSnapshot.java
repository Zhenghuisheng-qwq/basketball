package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketLeaderboardSnapshot {
    private Long id;
    private Long runId;
    private Long divisionId;
    private String scopeKey;
    private Long policyId;
    private String sourceRevision;
    private Date publishedAt;
    private Integer isPublic;
    private String publicScopeKey;
    private Date createTime;
}
