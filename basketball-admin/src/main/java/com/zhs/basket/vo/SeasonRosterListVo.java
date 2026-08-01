package com.zhs.basket.vo;

import lombok.Data;

@Data
public class SeasonRosterListVo {
    private Long id; private String rosterNo; private Long seasonId; private Long eventTeamId; private String eventTeamNo; private String eventTeamName; private Long teamId; private Long playerId; private Long memberId; private String playerNicknameSnapshot; private String platformSnapshot; private String gameIdDisplaySnapshot; private String rosterRole; private Integer isCaptain; private Integer isStarting; private Integer isSubstitute; private String sourceType; private String sourceTypeName; private Integer rosterStatus; private String rosterStatusName; private Integer lockStatus; private String lockStatusName; private String activeSlot; private java.util.Date effectiveTime; private java.util.Date invalidTime;
}
