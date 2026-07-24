package com.zhs.basket.vo;

import lombok.Data;

@Data
public class AvailableMatchPlayerVo {
    private Long registrationPlayerId;
    private Long playerId;
    private Long memberId;
    private String playerName;
    private String playerNo;
    private String jerseyNo;
    private String rosterRole;
    private Integer isStarting;
    private Integer isCaptain;
}
