package com.zhs.response.v4.publicapi;

import lombok.Data;

@Data
public class V4PublicMatchPeriodScoreResp {
    private Integer periodNo;
    private Integer homeScore;
    private Integer awayScore;
}