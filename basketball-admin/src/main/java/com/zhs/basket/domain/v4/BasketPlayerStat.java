package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * CNPA 赛事域 V4 实体：BasketPlayerStat
 */
@Data
public class BasketPlayerStat {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long eventTeamId;
    private Long eventPlayerId;
    private Integer matchCount;
    private Integer points;
    private BigDecimal avgPoints;
    private Integer rebound;
    private BigDecimal avgRebound;
    private Integer assist;
    private BigDecimal avgAssist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
