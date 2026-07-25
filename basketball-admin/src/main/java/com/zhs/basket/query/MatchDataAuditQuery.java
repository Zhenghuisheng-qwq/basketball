package com.zhs.basket.query;

import lombok.Data;

@Data
public class MatchDataAuditQuery {
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long teamId;
    private String auditStatus;
    private Integer dataStatus;
}
