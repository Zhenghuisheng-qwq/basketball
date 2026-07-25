package com.zhs.basket.vo;

import lombok.Data;

@Data
public class EventTeamListVo {
    private Long id; private Long seasonId; private String seasonName; private Long teamId; private String teamNo; private Long registrationId; private String eventTeamNo; private String teamNameSnapshot; private Long captainPlayerId; private Integer participationStatus; private String participationStatusName; private Integer rosterLockStatus; private String rosterLockStatusName; private java.util.Date approvedTime;
}
