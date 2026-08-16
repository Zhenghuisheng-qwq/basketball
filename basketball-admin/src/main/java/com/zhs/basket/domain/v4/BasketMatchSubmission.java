package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatchSubmission
 */
@Data
public class BasketMatchSubmission {
    private Long id;
    private Long matchId;
    private Long seasonId;
    private Long divisionId;
    private Long submitEventTeamId;
    private Long opponentEventTeamId;
    private Long submitEventPlayerId;
    private Long submitMemberId;
    private String submitterRole;
    private String submissionStatus;
    private Integer versionNo;
    private Integer homeScore;
    private Integer awayScore;
    private Date submitTime;
    private Long auditUserId;
    private String auditUserName;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
    private String submitRemark;
    private Long seriesSubmissionId;
    private String submissionMode;
    private Integer draftComplete;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
