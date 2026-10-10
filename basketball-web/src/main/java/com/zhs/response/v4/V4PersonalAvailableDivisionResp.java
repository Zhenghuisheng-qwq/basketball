package com.zhs.response.v4;

import lombok.Data;

import java.util.Date;

@Data
public class V4PersonalAvailableDivisionResp {
    private Long seasonId;
    private Long divisionId;
    private String seriesName;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private String registrationMode;
    private String gameMode;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Integer maxPersonCount;
    private Integer approvedPersonCount;
    private Integer remainingPersonCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private String myRegistrationStatus;
    private Boolean canRegister;
    private String cannotRegisterReason;
    private String cannotRegisterReasonCode;
}
