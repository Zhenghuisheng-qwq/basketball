package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalRegistration {
    private Long id;
    private Long divisionId;
    private Long seasonId;
    private Long seriesId;
    private Long userId;
    private Long playerId;
    private Long playerPlatformId;
    private String actualPlatformCode;
    private String gameAccountId;
    private String preferredPosition;
    private String registrationStatus;
    private Date submitTime;
    private Date approvedTime;
    private String rejectReason;
    private Long assignedDrawTeamId;
    private Long convertedRegistrationId;
    private String activeUniqueKey;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
