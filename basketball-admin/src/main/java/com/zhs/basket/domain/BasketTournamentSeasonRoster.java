package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentSeasonRoster {
    private Long id;
    private String rosterNo;
    private Long seasonId;
    private Long eventTeamId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private Long registrationId;
    private Long registrationPlayerId;
    private Long openRegistrationId;
    private String playerNicknameSnapshot;
    private Long avatarFileIdSnapshot;
    private String platformSnapshot;
    private String gameIdDisplaySnapshot;
    private String gameIdNormalizedSnapshot;
    private String positionSnapshot;
    private String rosterRole;
    private Integer isCaptain;
    private Integer isStarting;
    private Integer isSubstitute;
    private String sourceType;
    private Integer rosterStatus;
    private Integer lockStatus;
    private String activeSlot;
    private Date effectiveTime;
    private Date invalidTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
