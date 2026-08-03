package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatchPeriodScore
 */
@Data
public class BasketMatchPeriodScore {
    private Long id;
    private Long matchId;
    private Long divisionId;
    private Integer periodNo;
    private Integer homeScore;
    private Integer awayScore;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
