package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchLineupPlayerVo {
    private Long id;
    private Long registrationPlayerId;
    private Long playerId;
    private String playerName;
    private String playerNo;
    private String jerseyNo;
    private String rosterRole;
    private Integer isStarting;
    private Integer captainFlag;
    private String positionCode;
    private Integer playStatus;
    private Integer sortNo;
}
