package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketRegistrationAudit
 */
@Data
public class BasketRegistrationAudit {
    private Long id;
    private Long registrationId;
    private Long divisionId;
    private Long seasonId;
    private String auditAction;
    private String auditResult;
    private String beforeStatus;
    private String afterStatus;
    private Long auditUserId;
    private String auditUserName;
    private Date auditTime;
    private String auditRemark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
