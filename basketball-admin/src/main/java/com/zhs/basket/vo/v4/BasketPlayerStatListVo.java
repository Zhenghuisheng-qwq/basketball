package com.zhs.basket.vo.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BasketPlayerStatListVo {
    private Long eventPlayerId;
    private Long eventTeamId;
    private String playerName;
    private String teamName;
    private Integer matchCount;
    private Integer points;
    private BigDecimal avgPoints;
    private Integer rebound;
    private BigDecimal avgRebound;
    private Integer assist;
    private BigDecimal avgAssist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
}
