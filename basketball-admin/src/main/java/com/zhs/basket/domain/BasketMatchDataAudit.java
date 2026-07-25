package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataAudit {
    private Long id;
    private Long matchId;
    private Integer dataVersion;
    private String auditType;
    private Long submitterId;
    private Date submitTime;
    private String auditStatus;
    private Long auditorId;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
    private Long prevAuditId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
