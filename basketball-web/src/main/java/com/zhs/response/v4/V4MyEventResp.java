package com.zhs.response.v4;

import lombok.Data;

import java.util.Date;

@Data
public class V4MyEventResp {
    private Long seasonId;
    private String seasonTitle;
    private String seriesCode;
    private String seriesName;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private String platformName;
    private String gameMode;
    private String gameModeName;
    private String registrationMode;
    private Long eventTeamId;
    private String eventTeamName;
    private String teamName;
    private Long teamLogoFileId;
    private String roleInTeam;
    private String eventTeamStatus;
    private String divisionStatus;
    private Long currentStageId;
    private String currentStageName;
    private String stageType;
    private String stageExecutionStatus;
    private Long currentGroupId;
    private String currentGroupName;
    private Integer standingRank;
    private Integer standingPoints;
    private String qualificationStatus;
    private String qualificationStatusName;
    private Long nextMatchId;
    private Date nextMatchTime;
    private Long latestMatchId;
    private String latestMatchResult;
    private Boolean legacyEvent;
    private Integer matchCount;
    private Integer finishedMatchCount;
    private Integer pendingSubmissionCount;
}
