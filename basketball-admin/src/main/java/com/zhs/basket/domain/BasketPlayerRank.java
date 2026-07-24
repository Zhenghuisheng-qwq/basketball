package com.zhs.basket.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketPlayerRank {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private String rankType;
    private Long playerId;
    private Long teamId;
    private Integer rankNo;
    private Integer previousRank;
    private BigDecimal statValue;
    private Date snapshotDate;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
