package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TeamMembershipChangeListVo {
    private Long id;
    private String changeNo;
    private Long teamId;
    private String teamNo;
    private String teamName;
    private Long divisionId;
    private String divisionName;
    private Long registrationId;
    private String changeType;
    private String changeTypeName;
    private Long subjectPlayerId;
    private String subjectName;
    private String subjectPlayerNo;
    private String gameId;
    private String initiatorRole;
    private String initiatorRoleName;
    private String initiatorName;
    private String changeStatus;
    private String changeStatusName;
    private String effectStage;
    private String effectStageName;
    private String impactSummary;
    private String beforeSummary;
    private String afterSummary;
    private Date createTime;
    private Date auditTime;
    private Date effectiveTime;
    private String auditUserName;
    private String auditRemark;
    private Integer legacyRecord;
}
