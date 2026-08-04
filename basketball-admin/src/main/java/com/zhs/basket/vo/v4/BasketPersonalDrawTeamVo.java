package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BasketPersonalDrawTeamVo {
    private Long drawTeamId;
    private Long drawBatchId;
    private Long divisionId;
    private String temporaryTeamCode;
    private String temporaryTeamName;
    private Long captainPersonalRegistrationId;
    private Long captainPlayerId;
    private String captainPlayerName;
    private Long captainUserId;
    private Integer expectedMemberCount;
    private Integer actualMemberCount;
    private Long longTermTeamId;
    private String longTermTeamName;
    private Long convertedRegistrationId;
    private String formationStatus;
    private Date confirmedTime;
    private List<BasketPersonalDrawAssignmentVo> assignments;
}
