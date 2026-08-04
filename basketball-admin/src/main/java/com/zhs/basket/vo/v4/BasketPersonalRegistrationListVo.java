package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalRegistrationListVo {
    private Long personalRegistrationId;
    private Long seasonId;
    private String seasonTitle;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private Long playerId;
    private String playerName;
    private String actualPlatformCode;
    private String preferredPosition;
    private String registrationStatus;
    private Date submitTime;
    private Date approvedTime;
    private String rejectReason;
}
