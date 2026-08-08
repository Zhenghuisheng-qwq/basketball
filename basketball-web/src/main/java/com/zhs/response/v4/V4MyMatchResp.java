package com.zhs.response.v4;

import lombok.Data;
import java.util.Date;

@Data
public class V4MyMatchResp {
    private Long matchId;
    private Long seasonId;
    private String seasonCode;
    private String seasonTitle;
    private Long seriesId;
    private String seriesCode;
    private String seriesName;
    private Long divisionId;
    private String divisionCode;
    private String divisionName;
    private String platformCode;
    private String stageCode;
    private Long stageId;
    private String stageName;
    private String stageType;
    private Long groupId;
    private String groupName;
    private Integer roundNo;
    private String roundName;
    private String matchNo;
    private Date matchTime;
    private Long homeEventTeamId;
    private String homeTeamName;
    private String homeTeamShortName;
    private Long homeTeamLogoFileId;
    private Long awayEventTeamId;
    private String awayTeamName;
    private String awayTeamShortName;
    private Long awayTeamLogoFileId;
    private Long myEventTeamId;
    private Long opponentEventTeamId;
    private String currentSide;
    private String matchStatus;
    private String publishStatus;
    private String mySubmissionStatus;
    private String opponentSubmissionStatus;
    private Long myLatestSubmissionId;
    private Boolean canSubmit;
    private Boolean canSubmitData;
    private Boolean canEditSubmission;
    private Boolean canViewSubmission;
    private String cannotSubmitReason;
    private String userActionType;
    private String userActionText;
    private Integer latestVersionNo;
    private String latestAuditRemark;
    private String latestRejectReason;
    private Integer homeScore;
    private Integer awayScore;

    /** BO3/BO5 系列赛 ID（与赛事系列 seriesId 区分） */
    private Long matchSeriesId;
    private String matchSeriesCode;
    private Integer seriesGameNo;
    private Integer bestOf;
    private Integer seriesHomeWinCount;
    private Integer seriesAwayWinCount;
    private String matchSeriesStatus;
    private String matchSeriesStatusName;
}
