package com.zhs.basket.vo;

import lombok.Data;

@Data
public class OpenRegistrationAuditRecordVo {
    private Long id; private String auditAction; private Integer beforeStatus; private Integer afterStatus; private String auditorName; private String auditRemark; private java.util.Date auditTime;
}
