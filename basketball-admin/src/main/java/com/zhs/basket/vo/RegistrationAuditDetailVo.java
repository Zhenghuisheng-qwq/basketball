package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class RegistrationAuditDetailVo {
    private Long id;
    private String registrationNo;
    private Long seasonId;
    private Long teamId;
    private Long captainMemberId;
    private String contactName;
    private String contactMobile;
    private Integer registrationStatus;
    private String registrationStatusName;
    private Integer lineupCount;
    private String remark;
    private Date submittedTime;
    private Date auditTime;
}
