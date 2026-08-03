package com.zhs.basket.vo.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BasketMatchSubmissionTeamStatVo {
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
}
