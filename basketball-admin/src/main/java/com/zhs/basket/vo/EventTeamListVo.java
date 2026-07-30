package com.zhs.basket.vo;

import lombok.Data;

@Data
public class EventTeamListVo {
    private Long id; private Long seasonId; private String seasonName; private String tournamentName; private Long teamId; private String teamNo; private Long registrationId; private String eventTeamNo; private String teamNameSnapshot; private String gameTerminal; private Long captainPlayerId; private String captainNameSnapshot; private Integer participationStatus; private String participationStatusName; private Integer rosterLockStatus; private String rosterLockStatusName; private Boolean rosterLockNeedsRepair; private java.util.Date approvedTime;
}
