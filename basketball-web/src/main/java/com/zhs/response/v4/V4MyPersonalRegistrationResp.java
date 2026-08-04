package com.zhs.response.v4;

import lombok.Data;

import java.util.Date;

@Data
public class V4MyPersonalRegistrationResp {
    private Long personalRegistrationId;
    private Long seasonId;
    private Long divisionId;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private String preferredPosition;
    private String registrationStatus;
    private Date submitTime;
    private Date approvedTime;
    private String rejectReason;
    private Long assignedDrawTeamId;
    private Long convertedRegistrationId;
}
