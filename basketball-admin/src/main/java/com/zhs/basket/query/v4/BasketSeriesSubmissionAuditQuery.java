package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketSeriesSubmissionAuditQuery {
    private Long seasonId;
    private Long divisionId;
    private Long matchSeriesId;
    private String status;
    private String teamName;
    private String seriesCode;
}
