package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class TeamRosterItemVo {
    private Long registrationId;
    private Long teamId;
    private String teamName;
    private Date submitTime;
    private String auditStatus;
    private String participationRole;
    private Integer candidateOrder;
    private Integer currentEligibleCount;
    private String eligibilityNote;
}
