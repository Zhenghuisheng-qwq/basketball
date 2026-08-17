package com.zhs.request.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class V4MatchSubmissionPlayerStatReq {
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
    /** FGM：总运动战命中，含三分。2PM = FGM - 3PM */
    private Integer fieldGoalMade;
    /** FGA：总运动战出手，含三分。2PA = FGA - 3PA */
    private Integer fieldGoalAttempt;
    /** 3PM：三分命中，是 FGM 的子集 */
    private Integer threePointMade;
    /** 3PA：三分出手，是 FGA 的子集 */
    private Integer threePointAttempt;
    /** FTM：罚球命中 */
    private Integer freeThrowMade;
    /** FTA：罚球出手 */
    private Integer freeThrowAttempt;
    private BigDecimal minutes;
}
