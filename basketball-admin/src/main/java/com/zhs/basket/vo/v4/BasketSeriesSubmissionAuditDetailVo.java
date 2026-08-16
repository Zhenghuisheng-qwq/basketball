package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BasketSeriesSubmissionAuditDetailVo {
    private Long seriesSubmissionId;
    private Long matchSeriesId;
    private String seriesCode;
    private Integer bestOf;
    private Integer roundNo;
    private String divisionName;
    private Long homeEventTeamId;
    private String homeTeamName;
    private Long awayEventTeamId;
    private String awayTeamName;
    private Long submitEventTeamId;
    private String submitTeamName;
    private String declaredWinnerTeamName;
    private Integer declaredHomeWinCount;
    private Integer declaredAwayWinCount;
    private Integer actualGameCount;
    private Integer versionNo;
    private String status;
    private Date submitTime;
    private String rejectReason;
    private String auditRemark;
    private Date auditTime;
    private String auditUserName;
    private List<BasketSeriesSubmissionGameAuditVo> games;
    private List<BasketMatchSubmissionVersionVo> historyVersions;
}
