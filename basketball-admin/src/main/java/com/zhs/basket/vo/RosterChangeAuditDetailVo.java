package com.zhs.basket.vo;

import lombok.Data;

@Data
public class RosterChangeAuditDetailVo {
    private Long id; private String requestNo; private Long seasonId; private String seasonName; private String changeType; private String changeTypeName; private Integer requestStatus; private String requestStatusName; private Long applicantMemberId; private Long playerId; private String playerNickname; private Long sourceEventTeamId; private Long targetEventTeamId; private Long sourceRosterId; private Long newCaptainPlayerId; private String targetRosterRole; private Integer targetIsStarting; private Integer targetIsSubstitute; private String applyReason; private String rejectReason; private java.util.Date submitTime; private java.util.Date reviewedTime; private String reviewRemark; private Long resultRosterId;
}
