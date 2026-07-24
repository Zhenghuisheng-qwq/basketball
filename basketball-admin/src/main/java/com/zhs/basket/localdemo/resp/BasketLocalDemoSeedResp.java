package com.zhs.basket.localdemo.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BasketLocalDemoSeedResp {
    private String marker;
    private boolean alreadyExists;
    private boolean success;
    private String message;
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
    private int announcementCount;
    private int recommendationCount;
    private int avatarSyncUpdated;
    private int avatarSyncSkipped;
    private int avatarMissingBefore;
    private int avatarMissingAfter;
    private Long startedTime;
    private Long finishedTime;
    private Long durationMillis;
    private List<String> completedStages = new ArrayList<>();
    private List<String> skippedStages = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> validationErrors = new ArrayList<>();
}
