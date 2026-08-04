package com.zhs.response.v4;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class V4PersonalFormationTaskResp {
    private Long drawTeamId;
    private Long drawBatchId;
    private Long divisionId;
    private String divisionName;
    private String temporaryTeamCode;
    private String temporaryTeamName;
    private Integer expectedMemberCount;
    private Integer actualMemberCount;
    private Long longTermTeamId;
    private String longTermTeamName;
    private String formationStatus;
    private Long convertedRegistrationId;
    private Date confirmedTime;
    private List<V4PersonalFormationMemberResp> members;
}
