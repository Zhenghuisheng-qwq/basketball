package com.zhs.response.v4;

import lombok.Data;

@Data
public class V4MatchSubmissionSeriesContextResp {
    private Long matchSeriesId;
    private String seriesCode;
    private Integer bestOf;
    private Integer seriesGameNo;
    private Integer homeWinCount;
    private Integer awayWinCount;
    private String seriesStatus;
    private String seriesStatusName;
    private String homeTeamName;
    private String awayTeamName;
    private Boolean skipped;
    private String skippedReason;
}
