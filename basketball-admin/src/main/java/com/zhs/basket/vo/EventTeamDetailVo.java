package com.zhs.basket.vo;

import lombok.Data;

@Data
public class EventTeamDetailVo {
    private Long id; private Long seasonId; private String seasonName; private String tournamentName; private Long teamId; private String teamNo; private Long registrationId; private String eventTeamNo; private String teamNameSnapshot; private String shortNameSnapshot; private String teamRegionSnapshot; private String descriptionSnapshot; private String gameTerminal; private String registrationSource; private Long captainPlayerId; private String captainNameSnapshot; private String displayName; private Long logoFileId; private Integer participationStatus; private String participationStatusName; private Integer rosterLockStatus; private String rosterLockStatusName; private java.util.Date rosterLockedTime; private java.util.Date approvedTime; private String remark;
}
