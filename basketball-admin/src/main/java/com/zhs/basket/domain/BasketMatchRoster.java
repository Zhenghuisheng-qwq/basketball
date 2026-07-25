package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatchRoster {
    private Long id;
    private Long matchId;
    private Long seasonId;
    private Long teamId;
    private Long playerId;
    private String playerNicknameSnapshot;
    private String platformSnapshot;
    private String gameIdDisplaySnapshot;
    private Long registrationPlayerId;
    private Long seasonRosterId;
    private Long eventTeamId;
    private String jerseyNo;
    private String rosterRole;
    private Integer isStarting;
    private String positionCode;
    private Integer playStatus;
    private Integer captainFlag;
    private Integer rosterStatus;
    private Date lockedTime;
    private Integer dataVersion;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
