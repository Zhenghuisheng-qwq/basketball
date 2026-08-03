package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatchSubmissionTeamStat
 */
@Data
public class BasketMatchSubmissionTeamStat {
    private Long id;
    private Long submissionId;
    private Long matchId;
    private Long divisionId;
    private Long eventTeamId;
    private Integer score;
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
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
