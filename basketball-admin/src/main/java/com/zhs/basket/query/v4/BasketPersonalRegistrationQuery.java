package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketPersonalRegistrationQuery {
    private Long seasonId;
    private Long divisionId;
    private String registrationStatus;
    private String playerName;
}
