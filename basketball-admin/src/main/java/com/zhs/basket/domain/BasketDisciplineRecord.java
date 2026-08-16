package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDisciplineRecord {
    private Long id;
    private String recordNo;
    private String targetType;
    private Long seasonId;
    private Long matchId;
    private Long teamId;
    private Long eventTeamId;
    private Long playerId;
    private Long memberId;
    private String disciplineType;
    private String penaltyCode;
    private String title;
    private String reasonText;
    private String penaltyContent;
    private Integer matchSuspensionCount;
    private Integer matchSuspensionRemaining;
    private Integer pointDeduction;
    private Integer disciplineStatus;
    private String recordStatus;
    private String executionStatus;
    private Date executedTime;
    private String executionError;
    private Integer executionMatchForfeitCount;
    private Integer executionSeriesForfeitCount;
    private Integer executionQualificationRemovedCount;
    private Integer executionWarningCount;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Long decisionBy;
    private Date decisionTime;
    private Long approvedBy;
    private Date approvedTime;
    private Long revokedBy;
    private Date revokedTime;
    private String revokeReason;
    private String sourceType;
    private String idempotencyKey;
    private Long announcementId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
