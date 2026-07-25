package com.zhs.basket.query;

import lombok.Data;

@Data
public class RosterChangeAuditQuery {
    private Long seasonId;
    private String changeType;
    private Integer requestStatus;
}
