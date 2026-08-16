package com.zhs.request.v4;

import lombok.Data;

import java.util.List;

@Data
public class V4SeriesSubmissionSaveGameReq {
    private Long matchSeriesId;
    private Long matchId;
    private Long submitEventTeamId;
    private Integer declaredHomeWinCount;
    private Integer declaredAwayWinCount;
    private Integer homeScore;
    private Integer awayScore;
    private List<V4MatchSubmissionPlayerStatReq> homePlayerStats;
    private List<V4MatchSubmissionPlayerStatReq> awayPlayerStats;
    private List<V4MatchSubmissionPeriodScoreReq> periodScores;
    private List<Long> materialFileIds;
    private String submitRemark;
    private Boolean draftComplete;
}
