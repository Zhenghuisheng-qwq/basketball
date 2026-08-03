package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketRegistrationAuditQuery {
    private Long seasonId;
    private Long divisionId;
    private String registrationStatus;
    private String teamName;
    private String captainPlayerName;
}
