package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * CNPA 赛事域 V4 实体：BasketTeamStat
 */
@Data
public class BasketTeamStat {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long eventTeamId;
    private Integer matchCount;
    private Integer winCount;
    private Integer loseCount;
    private Integer pointsFor;
    private Integer pointsAgainst;
    private BigDecimal avgPointsFor;
    private BigDecimal avgPointsAgainst;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
