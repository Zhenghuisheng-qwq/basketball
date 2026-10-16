package com.zhs.response.v4;

import lombok.Data;
import java.util.Date;

@Data
public class V4MyRegistrationResp {
    private Long registrationId;
    private Long seasonId;
    private Long divisionId;
    private Long teamId;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private String teamName;
    private String registrationStatus;
    private String participationRole;
    private Integer lineupCount;
    private Integer currentEligibleCount;
    private Date submitTime;
    private Date approveTime;
    private String rejectReason;
}
