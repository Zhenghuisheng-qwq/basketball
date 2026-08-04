package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketStageStandingRowVo {
    private Long eventTeamId;
    private String teamName;
    private Integer rankNo;
    private Integer winCount;
    private Integer loseCount;
    private Integer pointFor;
    private Integer pointAgainst;
    private Integer pointDiff;
    private Integer standingPoints;
    private String rankExplainJson;
    private Integer manualRankPending;
}
