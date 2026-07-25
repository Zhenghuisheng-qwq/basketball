package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class BusinessAuditListVo {
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
    private Date createTime;
}
