package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketV4MatchTeamStat
 */
@Data
public class BasketV4MatchTeamStat {
    private Long id;
    private Long matchId;
    private Long seasonId;
    private Long divisionId;
    private Long eventTeamId;
    private Long opponentEventTeamId;
    private Integer isWinner;
    private Integer score;
    private Integer pointsFor;
    private Integer pointsAgainst;
    private Integer fieldGoalMade;
    private Integer fieldGoalAttempt;
    private Integer threePointMade;
    private Integer threePointAttempt;
    private Integer freeThrowMade;
    private Integer freeThrowAttempt;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Integer foul;
    private Long sourceSubmissionId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
