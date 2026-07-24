package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentRegistration {
    private Long id;
    private String registrationNo;
    private Long seasonId;
    private Long teamId;
    private Long captainMemberId;
    private Long captainPlayerId;
    private String contactName;
    private String contactMobile;
    private String contactEmail;
    private Integer registrationStatus;
    private Integer lineupCount;
    private Date submittedTime;
    private Date auditTime;
    private Date withdrawalTime;
    private String withdrawalReason;
    private Long currentAuditorId;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
