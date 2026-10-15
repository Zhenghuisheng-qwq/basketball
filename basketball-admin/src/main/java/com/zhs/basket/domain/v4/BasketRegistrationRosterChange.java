package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketRegistrationRosterChange {
    private Long id;
    private Long registrationId;
    private Long divisionId;
    private Long seasonId;
    private Long teamId;
    private String changeStatus;
    private Integer baseLineupVersion;
    private Long submitMemberId;
    private Long auditUserId;
    private String auditRemark;
    private Date auditTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
