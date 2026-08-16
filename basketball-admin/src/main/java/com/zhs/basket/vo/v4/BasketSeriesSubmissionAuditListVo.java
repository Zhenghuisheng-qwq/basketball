package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeriesSubmissionAuditListVo {
    private Long seriesSubmissionId;
    private Long matchSeriesId;
    private String seriesCode;
    private Integer bestOf;
    private String divisionName;
    private String homeTeamName;
    private String awayTeamName;
    private String submitTeamName;
    private Integer declaredHomeWinCount;
    private Integer declaredAwayWinCount;
    private Integer actualGameCount;
    private Integer versionNo;
    private String status;
    private Date submitTime;
    private Date auditTime;
}
