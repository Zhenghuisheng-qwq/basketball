package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchExceptionAudit {
    private Long id;
    private Long applicationId;
    private Long matchId;
    private String auditAction;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String auditRemark;
    private String rejectReason;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
