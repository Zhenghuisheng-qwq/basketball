package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketLeaderboardRun {
    private Long id;
    private Long divisionId;
    private String scopeKey;
    private Long policyId;
    private String purpose;
    private String sourceRevision;
    private String state;
    private String errorMessage;
    private Date startedTime;
    private Date finishedTime;
    private Date createTime;
}
