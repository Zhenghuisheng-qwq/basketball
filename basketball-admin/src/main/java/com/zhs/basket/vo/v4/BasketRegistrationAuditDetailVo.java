package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class BasketRegistrationAuditDetailVo {
    private Long registrationId;
    private Long seasonId;
    private String seasonTitle;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private String registrationMode;
    private Long teamId;
    private String teamName;
    private Long captainPlayerId;
    private String captainPlayerName;
    private Long captainMemberId;
    private Integer lineupCount;
    private Integer currentEligibleCount;
    private Integer currentTeamMemberCount;
    private Integer submittedRosterCount;
    private String registrationStatus;
    private Date submitTime;
    private Date approveTime;
    private String rejectReason;
    private List<BasketRegistrationPlayerVo> players;
    private List<RegistrationGapMemberVo> pendingMembers;
    private List<BasketRegistrationAuditLogVo> audits;
}
