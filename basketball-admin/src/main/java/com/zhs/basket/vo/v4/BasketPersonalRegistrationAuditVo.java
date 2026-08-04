package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalRegistrationAuditVo {
    private String beforeStatus;
    private String afterStatus;
    private String auditAction;
    private String auditRemark;
    private Long operatorId;
    private Date operateTime;
}
