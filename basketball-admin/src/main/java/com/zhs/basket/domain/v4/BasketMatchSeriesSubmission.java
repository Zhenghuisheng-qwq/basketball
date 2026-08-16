package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchSeriesSubmission {
    private Long id;
    private Long matchSeriesId;
    private Long seasonId;
    private Long divisionId;
    private Long stageId;
    private Long submitEventTeamId;
    private Long submitMemberId;
    private Integer declaredHomeWinCount;
    private Integer declaredAwayWinCount;
    private Integer actualGameCount;
    private String status;
    private Integer versionNo;
    private Date submitTime;
    private Long auditUserId;
    private String auditUserName;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
