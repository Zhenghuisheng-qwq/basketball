package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class TeamAuditRecordVo {
    private Long id;
    private String auditAction;
    private Integer beforeStatus;
    private Integer afterStatus;
    private String auditorName;
    private String auditRemark;
    private Date auditTime;
}
