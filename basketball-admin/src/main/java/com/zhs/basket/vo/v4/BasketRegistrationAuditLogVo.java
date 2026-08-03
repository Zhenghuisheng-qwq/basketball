package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketRegistrationAuditLogVo {
    private String auditAction;
    private String auditResult;
    private String beforeStatus;
    private String afterStatus;
    private Long auditUserId;
    private String auditUserName;
    private Date auditTime;
    private String auditRemark;
}
