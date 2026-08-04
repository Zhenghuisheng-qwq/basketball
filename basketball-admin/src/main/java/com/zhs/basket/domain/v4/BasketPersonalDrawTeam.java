package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalDrawTeam {
    private Long id;
    private Long drawBatchId;
    private Long divisionId;
    private String temporaryTeamCode;
    private String temporaryTeamName;
    private Long captainPersonalRegistrationId;
    private Long captainPlayerId;
    private Long captainUserId;
    private Integer expectedMemberCount;
    private Integer actualMemberCount;
    private Long longTermTeamId;
    private Long convertedRegistrationId;
    private String formationStatus;
    private Long confirmedBy;
    private Date confirmedTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
