package com.zhs.basket.query;

import lombok.Data;

@Data
public class BusinessAuditQuery {
    private String bizModule;
    private Long tournamentId;
    private Long seasonId;
    private String objectType;
    private Long objectId;
    private String actionType;
    private Long operatorId;
    private String keyword;
}
