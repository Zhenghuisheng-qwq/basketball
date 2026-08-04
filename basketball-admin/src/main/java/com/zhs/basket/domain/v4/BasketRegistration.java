package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketRegistration
 */
@Data
public class BasketRegistration {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private String registrationMode;
    private String registrationSource;
    private Long sourcePersonalDrawTeamId;
    private Long sourcePersonalDrawBatchId;
    private Long teamId;
    private Long captainPlayerId;
    private Long captainMemberId;
    private String registrationStatus;
    private Integer lineupCount;
    private Date submitTime;
    private Date approveTime;
    private String rejectReason;
    private Date withdrawTime;
    private Long auditUserId;
    private String auditRemark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
