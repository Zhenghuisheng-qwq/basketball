package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPlayerPlatformIdentityHistory {
    private Long id;
    private Long userId;
    private Long playerId;
    private Long platformIdentityId;
    private String platformCode;
    private String changeType;
    private String oldGameId;
    private String newGameId;
    private Date effectiveTime;
    private Long auditRecordId;
    private Long operatorId;
    private String initialBindSlot;
    private Date createTime;
    private Integer isDelete;
}
