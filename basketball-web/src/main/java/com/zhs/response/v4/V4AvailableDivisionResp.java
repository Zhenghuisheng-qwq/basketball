package com.zhs.response.v4;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class V4AvailableDivisionResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;
    private String seriesName;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private String registrationMode;
    private String gameMode;
    private String divisionStatus;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationEndTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date matchStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date matchEndTime;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long grantTeamId;
    private String grantTeamName;
    private Integer maxTeamCount;
    private Integer approvedTeamCount;
    private Integer remainingTeamCount;
    private String myGrantStatus;
    private String myRegistrationStatus;
    private String participationRole;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long myRegistrationId;
    private Boolean canRegister;
    private Boolean canViewRegistration;
    private String cannotRegisterReason;
    private String cannotRegisterReasonCode;
    private String registrationWindowStatus;
    private String registrationWindowStatusLabel;
}
