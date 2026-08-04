package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalRegistrationAudit {
    private Long id;
    private Long personalRegistrationId;
    private Long divisionId;
    private String beforeStatus;
    private String afterStatus;
    private String auditAction;
    private String auditRemark;
    private Long operatorId;
    private Date operateTime;
    private Date createTime;
    private Integer isDelete;
}
