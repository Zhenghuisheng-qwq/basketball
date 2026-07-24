package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketPlayerCertAudit {
    private Long id;
    private Long applicationId;
    private String auditAction;
    private Integer beforeStatus;
    private Integer afterStatus;
    private Long auditorId;
    private String auditorName;
    private String auditRemark;
    private Date auditTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
