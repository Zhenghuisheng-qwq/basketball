package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SeasonSettlementSummaryVo {
    private Long seasonId;
    private String seasonName;
    private Long tournamentId;
    private String tournamentName;
    private Integer seasonStatus;
    private int confirmedMatchCount;
    private int unfinishedMatchCount;
    private int reopenedMatchCount;
    private int cancelledMatchCount;
    private int standingCount;
    private int teamSeasonStatCount;
    private int playerSeasonStatCount;
    private int teamRankCount;
    private int playerRankCount;
    private boolean standingRuleConfigured;
    private Date lastStandingTime;
    private Date lastTeamStatTime;
    private Date lastPlayerStatTime;
    private Date lastSettlementTime;
    private String lastOperatorName;
}
