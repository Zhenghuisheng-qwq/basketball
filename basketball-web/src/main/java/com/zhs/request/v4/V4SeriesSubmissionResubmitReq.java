package com.zhs.request.v4;

import lombok.Data;

@Data
public class V4SeriesSubmissionResubmitReq {
    private Long oldSeriesSubmissionId;
    private Long matchSeriesId;
    private Integer declaredHomeWinCount;
    private Integer declaredAwayWinCount;
}
