package com.zhs.request.v4;

import lombok.Data;

@Data
public class V4SeriesSubmissionDeclareReq {
    private Long matchSeriesId;
    private Integer declaredHomeWinCount;
    private Integer declaredAwayWinCount;
}
