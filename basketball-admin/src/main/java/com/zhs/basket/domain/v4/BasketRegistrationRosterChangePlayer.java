package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketRegistrationRosterChangePlayer {
    private Long id;
    private Long changeId;
    private Long playerId;
    private Integer isCaptain;
    private Integer isStarter;
    private Integer sortOrder;
    private Date createTime;
    private Integer isDelete;
}
