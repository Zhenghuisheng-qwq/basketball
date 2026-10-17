package com.zhs.basket.vo;

import lombok.Data;

@Data
public class DashboardSummaryVo {
    private Long currentTournamentId;
    private String currentTournamentName;
    private Integer pendingTeamAudit;
    private Integer pendingPlayerCert;
    private Integer pendingRegistration;
    private Integer pendingOpenRegistration;
    private Integer pendingRosterChange;
    private Integer pendingMembershipChange;
    private Integer pendingPlatformChange;
    private Integer pendingMatchDataAudit;
    private Integer pendingMatchDispute;
}
