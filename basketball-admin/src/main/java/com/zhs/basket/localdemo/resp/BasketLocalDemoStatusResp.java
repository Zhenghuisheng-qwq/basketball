package com.zhs.basket.localdemo.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BasketLocalDemoStatusResp {
    private boolean localProfileActive;
    private boolean featureEnabled;
    private String marker;
    private boolean dataExists;
    private boolean dataComplete;
    private boolean canSeed;
    private boolean canReset;
    private int accountCount;
    private int playerCount;
    private int captainCount;
    private int teamCount;
    private int teamMemberCount;
    private int tournamentCount;
    private int seasonCount;
    private int registrationCount;
    private int registrationPlayerCount;
    private int stageCount;
    private int roundCount;
    private int groupCount;
    private int groupTeamCount;
    private int matchCount;
    private int confirmedMatchCount;
    private int scheduledMatchCount;
    private int lineupCount;
    private int periodScoreCount;
    private int teamMatchStatCount;
    private int playerMatchStatCount;
    private int standingCount;
    private int teamSeasonStatCount;
    private int playerSeasonStatCount;
    private int teamRankCount;
    private int playerRankCount;
    private int announcementCount;
    private int recommendationCount;
    private int avatarMissingCount;
    private int avatarConfiguredCount;
    private List<String> missingStages = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
