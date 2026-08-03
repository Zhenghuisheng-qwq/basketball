package com.zhs.basket.vo.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BasketTeamStatListVo {
    private Long eventTeamId;
    private String teamName;
    private Integer matchCount;
    private Integer winCount;
    private Integer loseCount;
    private Integer pointsFor;
    private Integer pointsAgainst;
    private BigDecimal avgPointsFor;
    private BigDecimal avgPointsAgainst;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
}
