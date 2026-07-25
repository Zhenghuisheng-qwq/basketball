package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDisciplineRestriction {
    private Long id;
    private String subjectType;
    private Long subjectId;
    private Long seasonId;
    private Integer allowRegistration;
    private Integer allowTransfer;
    private Integer allowCaptain;
    private Integer allowLineup;
    private Integer allowParticipation;
    private Integer remainingMatchSuspensions;
    private Long effectiveDisciplineId;
    private Integer activeFlag;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
