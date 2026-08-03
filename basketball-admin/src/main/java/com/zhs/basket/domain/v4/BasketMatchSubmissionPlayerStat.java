package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * CNPA 赛事域 V4 实体：BasketMatchSubmissionPlayerStat
 */
@Data
public class BasketMatchSubmissionPlayerStat {
    private Long id;
    private Long submissionId;
    private Long matchId;
    private Long divisionId;
    private Long eventTeamId;
    private Long eventPlayerId;
    private Integer points;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Integer foul;
    private BigDecimal minutes;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
