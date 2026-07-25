package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentRosterChangeRequest {
    private Long id;
    private String requestNo;
    private Long seasonId;
    private String changeType;
    private Integer requestStatus;
    private Long applicantMemberId;
    private Long playerId;
    private Long sourceEventTeamId;
    private Long targetEventTeamId;
    private Long sourceRosterId;
    private Long newCaptainPlayerId;
    private String targetRosterRole;
    private Integer targetIsStarting;
    private Integer targetIsSubstitute;
    private String applyReason;
    private String rejectReason;
    private Date submitTime;
    private Date reviewedTime;
    private Long reviewerId;
    private String reviewRemark;
    private Long resultRosterId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
