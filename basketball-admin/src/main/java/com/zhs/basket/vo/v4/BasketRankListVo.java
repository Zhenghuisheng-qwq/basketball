package com.zhs.basket.vo.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BasketRankListVo {
    private String rankType;
    private Integer rankNo;
    private String targetType;
    private Long eventTeamId;
    private String teamName;
    private Long eventPlayerId;
    private String playerName;
    private BigDecimal rankValue;
}
