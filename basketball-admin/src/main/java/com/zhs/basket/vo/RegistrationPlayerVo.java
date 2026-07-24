package com.zhs.basket.vo;

import lombok.Data;

@Data
public class RegistrationPlayerVo {
    private Long id;
    private Long playerId;
    private Long memberId;
    private String rosterRole;
    private String jerseyNo;
    private Integer isCaptain;
    private Integer validationStatus;
    private String rejectReason;
}
