package com.zhs.request.v4;

import lombok.Data;

@Data
public class V4MatchSubmissionPeriodScoreReq {
    private Integer periodNo;
    private Integer homeScore;
    private Integer awayScore;
}
