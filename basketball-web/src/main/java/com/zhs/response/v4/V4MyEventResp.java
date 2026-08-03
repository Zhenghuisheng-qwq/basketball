package com.zhs.response.v4;

import lombok.Data;

@Data
public class V4MyEventResp {
    private Long seasonId;
    private String seasonTitle;
    private String seriesCode;
    private String seriesName;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private Long eventTeamId;
    private String teamName;
    private Long teamLogoFileId;
    private String roleInTeam;
    private String eventTeamStatus;
    private String divisionStatus;
    private Integer matchCount;
    private Integer finishedMatchCount;
    private Integer pendingSubmissionCount;
}
