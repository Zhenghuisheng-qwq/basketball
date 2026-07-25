package com.zhs.basket.query;

import lombok.Data;

@Data
public class EventTeamAdminQuery {
    private Long seasonId;
    private String eventTeamNo;
    private String teamName;
    private Integer participationStatus;
    private Integer rosterLockStatus;
}
