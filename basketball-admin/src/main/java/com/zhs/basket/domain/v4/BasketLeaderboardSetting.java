package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketLeaderboardSetting {
    private Long id;
    private Long featuredDivisionId;
    private Long updateBy;
    private Date updateTime;
}
