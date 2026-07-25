package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketPlayerPlatformAccount {
    private Long id;
    private Long playerId;
    private String platform;
    private String displayGameId;
    private String normalizedGameId;
    private Integer accountStatus;
    private Integer isPrimary;
    private Long sourceApplicationId;
    private Long approvedBy;
    private Date approvedTime;
    private Date effectiveTime;
    private Date invalidTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
