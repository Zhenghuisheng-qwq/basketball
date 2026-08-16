package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * CNPA 赛事域 V4 实体：BasketV4MatchPlayerStat
 */
@Data
public class BasketV4MatchPlayerStat {
    private Long id;
    private Long matchId;
    private Long seasonId;
    private Long divisionId;
    private Long eventTeamId;
    private Long eventPlayerId;
    private Integer points;
    private Integer score;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Integer foul;
    private Integer fieldGoalMade;
    private Integer fieldGoalAttempt;
    private Integer threePointMade;
    private Integer threePointAttempt;
    private Integer freeThrowMade;
    private Integer freeThrowAttempt;
    private BigDecimal minutes;
    private Long sourceSubmissionId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
