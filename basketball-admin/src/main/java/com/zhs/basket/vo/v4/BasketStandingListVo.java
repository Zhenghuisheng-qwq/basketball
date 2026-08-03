package com.zhs.basket.vo.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BasketStandingListVo {
    private Long eventTeamId;
    private String teamName;
    private Integer rankNo;
    private Integer winCount;
    private Integer loseCount;
    private Integer pointFor;
    private Integer pointAgainst;
    private Integer pointDiff;
    private Integer standingPoints;
}
