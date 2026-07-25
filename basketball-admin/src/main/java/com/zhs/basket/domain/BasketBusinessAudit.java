package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketBusinessAudit {
    private Long id;
    private String auditNo;
    private String bizModule;
    private Long tournamentId;
    private Long seasonId;
    private String objectType;
    private Long objectId;
    private String bizNo;
    private String actionType;
    private String actionResult;
    private String changeSummary;
    private String actionReason;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private Long relatedApplyId;
    private Long relatedAuditId;
    private Long relatedDisciplineId;
    private Long relatedMatchId;
    private Integer objectVersion;
    private String traceId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
