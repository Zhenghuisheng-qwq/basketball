package com.zhs.response.v4;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class V4SeriesSubmissionWorkspaceResp {
    private Long matchSeriesId;
    private String seriesCode;
    private Integer bestOf;
    private Integer roundNo;
    private String seasonTitle;
    private String divisionName;
    private String stageName;
    private Long homeEventTeamId;
    private String homeTeamName;
    private Long awayEventTeamId;
    private String awayTeamName;
    private Long seriesSubmissionId;
    private Integer versionNo;
    private String status;
    private Integer declaredHomeWinCount;
    private Integer declaredAwayWinCount;
    private Integer actualGameCount;
    private Long submitEventTeamId;
    private String submitTeamName;
    private Date submitTime;
    private String rejectReason;
    private String auditRemark;
    private Integer completedGameCount;
    private Boolean canEdit;
    private Boolean canSubmitSeries;
    private List<V4MatchSubmissionEligiblePlayerResp> homeEligiblePlayers;
    private List<V4MatchSubmissionEligiblePlayerResp> awayEligiblePlayers;
    private List<V4SeriesSubmissionGameResp> games;
}

