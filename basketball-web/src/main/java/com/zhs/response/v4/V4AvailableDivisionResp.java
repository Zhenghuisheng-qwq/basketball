package com.zhs.response.v4;

import lombok.Data;
import java.util.Date;

@Data
public class V4AvailableDivisionResp {
    private Long seasonId;
    private Long divisionId;
    private String seriesName;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private Long grantTeamId;
    private String grantTeamName;
    private Integer maxTeamCount;
    private Integer approvedTeamCount;
    private Integer remainingTeamCount;
    private String myGrantStatus;
    private String myRegistrationStatus;
    private Boolean canRegister;
    private String cannotRegisterReason;
}
