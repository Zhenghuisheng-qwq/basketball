package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MatchDataSubmissionAuditLogVo {
    private Long id;
    private Long submissionId;
    private String auditAction;
    private String auditActionName;
    private String beforeStatus;
    private String afterStatus;
    private String auditStatus;
    private Long auditorId;
    private String auditorName;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
}
