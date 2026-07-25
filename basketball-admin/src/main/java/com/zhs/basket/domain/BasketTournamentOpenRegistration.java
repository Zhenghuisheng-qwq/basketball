package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentOpenRegistration {
    private Long id;
    private String registrationNo;
    private Long seasonId;
    private Long memberId;
    private Long playerId;
    private String platform;
    private String displayGameId;
    private String normalizedGameId;
    private String positionCode;
    private Integer registrationStatus;
    private Date submittedTime;
    private Date auditTime;
    private Date withdrawalTime;
    private String withdrawalReason;
    private Long currentAuditorId;
    private String remark;
    private Long resultRosterId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
