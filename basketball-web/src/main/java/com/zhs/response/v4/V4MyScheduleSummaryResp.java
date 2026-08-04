package com.zhs.response.v4;

import lombok.Data;

@Data
public class V4MyScheduleSummaryResp {
    private Integer upcomingCount;
    private Integer pendingSubmitCount;
    private Integer reviewingCount;
    private Integer finishedCount;
    private Integer totalCount;
}
