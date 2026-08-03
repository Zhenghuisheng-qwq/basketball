package com.zhs.response.v4;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class V4MyScheduleResp extends V4MyMatchResp {
    private Integer homeScore;
    private Integer awayScore;
    private String opponentSubmissionStatus;
}
