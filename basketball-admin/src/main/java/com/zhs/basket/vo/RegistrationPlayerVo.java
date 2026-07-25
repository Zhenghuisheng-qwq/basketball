package com.zhs.basket.vo;

import lombok.Data;

@Data
public class RegistrationPlayerVo {
    private Long id;
    private Long playerId;
    private Long memberId;
    private String playerNickname;
    private String playerNo;
    private String rosterRole;
    private String jerseyNo;
    private Integer isCaptain;
    private Integer isStarting;
    private Integer validationStatus;
    private String rejectReason;
}
