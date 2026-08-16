package com.zhs.response.v4;

import lombok.Data;

import java.util.List;

@Data
public class V4SeriesSubmissionGameResp {
    private Long matchId;
    private Integer seriesGameNo;
    private String matchNo;
    private Long submissionId;
    private String submissionStatus;
    private Integer draftComplete;
    private Integer homeScore;
    private Integer awayScore;
    private String submitRemark;
    private Boolean required;
    private Boolean skipped;
    private V4MatchSubmissionTeamStatResp homeTeamStat;
    private V4MatchSubmissionTeamStatResp awayTeamStat;
    private List<V4MatchSubmissionPlayerStatResp> homePlayerStats;
    private List<V4MatchSubmissionPlayerStatResp> awayPlayerStats;
    private List<V4MatchSubmissionPeriodScoreResp> periodScores;
    private List<Long> materialFileIds;
}
