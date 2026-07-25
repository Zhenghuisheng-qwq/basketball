package com.zhs.basket.query;

import lombok.Data;

@Data
public class RosterOperationLogQuery {
    private Long seasonId;
    private Long eventTeamId;
    private String operationType;
}
