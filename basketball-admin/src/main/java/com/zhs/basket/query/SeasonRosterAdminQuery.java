package com.zhs.basket.query;

import lombok.Data;

@Data
public class SeasonRosterAdminQuery {
    private Long seasonId;
    private Long eventTeamId;
    private Long playerId;
    private Integer rosterStatus;
}
