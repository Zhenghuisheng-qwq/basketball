package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketTeamMembershipChange {
    private Long id;
    private String changeNo;
    private Long teamId;
    private Long divisionId;
    private Long registrationId;
    private String changeType;
    private Long subjectPlayerId;
    private Long subjectMemberId;
    private Long initiatorMemberId;
    private String initiatorRole;
    private String sourceBizType;
    private Long sourceBizId;
    private String beforeSummary;
    private String afterSummary;
    private String changeStatus;
    private String effectStage;
    private String impactSummary;
    private Long auditUserId;
    private String auditUserName;
    private String auditRemark;
    private Date auditTime;
    private Date effectiveTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
