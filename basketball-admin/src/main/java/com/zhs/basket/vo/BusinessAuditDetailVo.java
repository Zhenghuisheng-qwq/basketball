package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BusinessAuditDetailVo {
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
    private Long relatedDisciplineId;
    private Long relatedMatchId;
    private Date createTime;
    private List<BusinessAuditFieldVo> fields;
}
