package com.zhs.basket.vo.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BasketMatchSubmissionPlayerStatVo {
    private Long eventTeamId;
    private Long eventPlayerId;
    private String playerName;
    private Integer points;
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
}
