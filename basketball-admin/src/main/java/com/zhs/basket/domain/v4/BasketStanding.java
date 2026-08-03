package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketStanding
 */
@Data
public class BasketStanding {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long eventTeamId;
    private Integer rankNo;
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;
    private Integer pointFor;
    private Integer pointAgainst;
    private Integer pointDiff;
    private Integer standingPoints;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
